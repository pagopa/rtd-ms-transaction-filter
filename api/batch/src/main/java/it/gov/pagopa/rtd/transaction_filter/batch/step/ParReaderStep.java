package it.gov.pagopa.rtd.transaction_filter.batch.step;

import it.gov.pagopa.rtd.transaction_filter.batch.config.BatchConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.ParReaderMasterStepListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.PGPFlatFileItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.ParStoreWriter;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.ParWriter;
import it.gov.pagopa.rtd.transaction_filter.service.ParStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.WriterTrackerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutorService;

@Configuration
@DependsOn({"partitionerTaskExecutor","readerTaskExecutor"})
@RequiredArgsConstructor
@Data
@PropertySource("classpath:config/parReaderStep.properties")
public class ParReaderStep {

    @Value("${batchConfiguration.TransactionFilterBatch.parList.partitionerSize}")
    private Integer partitionerSize;
    @Value("${batchConfiguration.TransactionFilterBatch.parList.chunkSize}")
    private Integer chunkSize;
    @Value("${batchConfiguration.TransactionFilterBatch.parList.skipLimit}")
    private Integer skipLimit;
    @Value("${batchConfiguration.TransactionFilterBatch.parList.parDirectoryPath}")
    private String parDirectoryPath;
    @Value("${batchConfiguration.TransactionFilterBatch.parList.secretKeyPath}")
    private String secretKeyPath;
    @Value("${batchConfiguration.TransactionFilterBatch.parList.passphrase}")
    private String passphrase;
    @Value("${batchConfiguration.TransactionFilterBatch.parList.applyDecrypt}")
    private Boolean applyDecrypt;
    @Value("${batchConfiguration.TransactionFilterBatch.parList.applyHashing}")
    private Boolean applyPanListHashing;
    @Value("${batchConfiguration.TransactionFilterBatch.parList.poolSize}")
    private Integer executorPoolSize;

    private final BatchConfig batchConfig;
    private final StepBuilderFactory stepBuilderFactory;
    private ExecutorService executorService;


    /**
     *
     * @param file
     *          Late-Binding parameter to be used as the resource for the reader instance
     * @return instance of the itemReader to be used in the first step of the configured job
     */
    @SneakyThrows
    @Bean
    @StepScope
    public PGPFlatFileItemReader parItemReader(
            @Value("#{stepExecutionContext['fileName']}") String file) {
        PGPFlatFileItemReader flatFileItemReader = new PGPFlatFileItemReader(
                secretKeyPath, passphrase, applyDecrypt);
        flatFileItemReader.setResource(new UrlResource(file));
        flatFileItemReader.setLineMapper(new PassThroughLineMapper());
        return flatFileItemReader;
    }


    /**
     *
     * @return instance of the itemWriter to be used in the first step of the configured job
     */
    @Bean
    @StepScope
    public ParWriter parItemWriter(ParStoreService parStoreService, WriterTrackerService writerTrackerService) {
        return new ParWriter(parStoreService, writerTrackerService);
    }

    /**
     *
     * @return instance of the itemWriter to be used in the first step of the configured job
     */
    @Bean
    @StepScope
    public ParStoreWriter parStoreItemWriter(ParStoreService parStoreService) {
        return new ParStoreWriter(parStoreService);
    }

    /**
     *
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws Exception
     */
    @Bean
    @JobScope
    public Partitioner parRecoveryPartitioner() throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(parDirectoryPath));
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     *
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws Exception
     */
    @Bean
    @JobScope
    public Partitioner parStoreRecoveryPartitioner(
            @Value("#{jobParameters['workingParDirectory']}") String workingParDirectory) throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(workingParDirectory.concat("/current/*.csv")));
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     *
     * @return master step to be used as the formal main step in the reading phase of the job,
     * partitioned for scalability on multiple file reading
     * @throws Exception
     */
    @Bean
    public Step parStoreRecoveryMasterStep(ParStoreService parStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("par-store-recovery-master-step")
                .partitioner(parStoreRecoveryWorkerStep(parStoreService, writerTrackerService))
                .partitioner("partition", parStoreRecoveryPartitioner(null))
                .taskExecutor(batchConfig.partitionerTaskExecutor())
                .listener(parReaderMasterStepListener(parStoreService, writerTrackerService))
                .build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step parStoreRecoveryWorkerStep(ParStoreService parStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("par-store-recovery-worker-step")
                .<String, String>chunk(chunkSize)
                .reader(parItemReader(null))
                .writer(parStoreItemWriter(parStoreService))
                .faultTolerant()
                .skipLimit(skipLimit)
                .noSkip(FileNotFoundException.class)
                .skip(Exception.class)
                .taskExecutor(batchConfig.readerTaskExecutor())
                .build();
    }

    /**
     *
     * @return master step to be used as the formal main step in the reading phase of the job,
     * partitioned for scalability on multiple file reading
     * @throws Exception
     */
    @Bean
    public Step parRecoveryMasterStep(ParStoreService parStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("par-recovery-master-step")
                .partitioner(parRecoveryWorkerStep(parStoreService, writerTrackerService))
                .partitioner("partition", parRecoveryPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor())
                .listener(parReaderMasterStepListener(parStoreService, writerTrackerService))
                .build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step parRecoveryWorkerStep(ParStoreService parStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("par-recovery-worker-step")
                .<String, String>chunk(chunkSize)
                .reader(parItemReader(null))
                .writer(parItemWriter(parStoreService, writerTrackerService))
                .faultTolerant()
                .skipLimit(skipLimit)
                .noSkip(FileNotFoundException.class)
                .skip(Exception.class)
                .noRetry(Exception.class)
                .noRollback(Exception.class)
                .taskExecutor(batchConfig.readerTaskExecutor())
                .build();
    }

    @Bean
    public ParReaderMasterStepListener parReaderMasterStepListener(
            ParStoreService parStoreService, WriterTrackerService writerTrackerService) {
        ParReaderMasterStepListener parReaderStepListener = new ParReaderMasterStepListener();
        parReaderStepListener.setParStoreService(parStoreService);
        parReaderStepListener.setWriterTrackerService(writerTrackerService);
        return parReaderStepListener;
    }

}
