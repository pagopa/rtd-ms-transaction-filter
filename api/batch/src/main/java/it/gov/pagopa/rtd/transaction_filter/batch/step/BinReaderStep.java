package it.gov.pagopa.rtd.transaction_filter.batch.step;

import it.gov.pagopa.rtd.transaction_filter.batch.config.BatchConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.BinReaderMasterStepListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.PGPFlatFileItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.BinStoreWriter;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.BinWriter;
import it.gov.pagopa.rtd.transaction_filter.service.BinStoreService;
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
@PropertySource("classpath:config/binReaderStep.properties")
public class BinReaderStep {

    @Value("${batchConfiguration.TokenPanFilterBatch.bin.partitionerSize}")
    private Integer partitionerSize;
    @Value("${batchConfiguration.TokenPanFilterBatch.bin.chunkSize}")
    private Integer chunkSize;
    @Value("${batchConfiguration.TokenPanFilterBatch.bin.skipLimit}")
    private Integer skipLimit;
    @Value("${batchConfiguration.TokenPanFilterBatch.bin.binDirectoryPath}")
    private String binDirectoryPath;
    @Value("${batchConfiguration.TokenPanFilterBatch.bin.secretKeyPath}")
    private String secretKeyPath;
    @Value("${batchConfiguration.TokenPanFilterBatch.bin.passphrase}")
    private String passphrase;
    @Value("${batchConfiguration.TokenPanFilterBatch.bin.applyDecrypt}")
    private Boolean applyDecrypt;
    @Value("${batchConfiguration.TokenPanFilterBatch.bin.applyHashing}")
    private Boolean applyPanListHashing;
    @Value("${batchConfiguration.TokenPanFilterBatch.bin.poolSize}")
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
    public PGPFlatFileItemReader binItemReader(
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
    public BinWriter binItemWriter(BinStoreService binStoreService, WriterTrackerService writerTrackerService) {
        return new BinWriter(binStoreService, writerTrackerService);
    }

    /**
     *
     * @return instance of the itemWriter to be used in the first step of the configured job
     */
    @Bean
    @StepScope
    public BinStoreWriter binStoreItemWriter(BinStoreService binStoreService) {
        return new BinStoreWriter(binStoreService);
    }

    /**
     *
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws Exception
     */
    @Bean
    @JobScope
    public Partitioner binRecoveryPartitioner() throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(binDirectoryPath));
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
    public Partitioner binStoreRecoveryPartitioner(
            @Value("#{jobParameters['workingBinDirectory']}") String workingBinDirectory) throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(workingBinDirectory.concat("/current/*.csv")));
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
    public Step binStoreRecoveryMasterStep(BinStoreService binStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("bin-store-recovery-master-step")
                .partitioner(binStoreRecoveryWorkerStep(binStoreService))
                .partitioner("partition", binStoreRecoveryPartitioner(null))
                .taskExecutor(batchConfig.partitionerTaskExecutor())
                .listener(binReaderMasterStepListener(binStoreService, writerTrackerService))
                .build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step binStoreRecoveryWorkerStep(BinStoreService binStoreService) throws Exception {
        return stepBuilderFactory.get("bin-store-recovery-worker-step")
                .<String, String>chunk(chunkSize)
                .reader(binItemReader(null))
                .writer(binStoreItemWriter(binStoreService))
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
    public Step binRecoveryMasterStep(BinStoreService binStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("bin-recovery-master-step")
                .partitioner(binRecoveryWorkerStep(binStoreService, writerTrackerService))
                .partitioner("partition", binRecoveryPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor())
                .listener(binReaderMasterStepListener(binStoreService, writerTrackerService))
                .build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step binRecoveryWorkerStep(BinStoreService binStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("bin-recovery-worker-step")
                .<String, String>chunk(chunkSize)
                .reader(binItemReader(null))
                .writer(binItemWriter(binStoreService, writerTrackerService))
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
    public BinReaderMasterStepListener binReaderMasterStepListener(
            BinStoreService binStoreService, WriterTrackerService writerTrackerService) {
        BinReaderMasterStepListener binReaderMasterStepListener = new BinReaderMasterStepListener();
        binReaderMasterStepListener.setBinStoreService(binStoreService);
        binReaderMasterStepListener.setWriterTrackerService(writerTrackerService);
        return binReaderMasterStepListener;
    }

}
