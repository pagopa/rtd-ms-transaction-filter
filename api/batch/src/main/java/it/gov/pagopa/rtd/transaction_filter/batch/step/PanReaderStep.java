package it.gov.pagopa.rtd.transaction_filter.batch.step;

import it.gov.pagopa.rtd.transaction_filter.batch.config.BatchConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.HpanReaderMasterStepListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.PGPFlatFileItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.HpanStoreWriter;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.HpanWriter;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@DependsOn({"partitionerTaskExecutor","readerTaskExecutor"})
@RequiredArgsConstructor
@Data
@PropertySource("classpath:config/panReaderStep.properties")
public class PanReaderStep {

    @Value("${batchConfiguration.TransactionFilterBatch.panList.partitionerSize}")
    private Integer partitionerSize;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.chunkSize}")
    private Integer chunkSize;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.skipLimit}")
    private Integer skipLimit;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.hpanDirectoryPath}")
    private String hpanDirectoryPath;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.hpanWorkerDirectoryPath}")
    private String hpanWorkerDirectoryPath;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.secretKeyPath}")
    private String secretKeyPath;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.passphrase}")
    private String passphrase;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.applyDecrypt}")
    private Boolean applyDecrypt;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.applyHashing}")
    private Boolean applyPanListHashing;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.poolSize}")
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
    public PGPFlatFileItemReader hpanItemReader(
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
    public HpanWriter hpanItemWriter(HpanStoreService hpanStoreService, WriterTrackerService writerTrackerService) {
        HpanWriter hpanWriter = new HpanWriter(hpanStoreService, writerTrackerService);
        hpanWriter.setExecutor(writerExecutor());
        return hpanWriter;
    }

    /**
     *
     * @return instance of the itemWriter to be used in the first step of the configured job
     */
    @Bean
    @StepScope
    public HpanStoreWriter hpanStoreItemWriter(HpanStoreService hpanStoreService) {
        return new HpanStoreWriter(hpanStoreService, this.applyPanListHashing);
    }

    /**
     *
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws Exception
     */
    @Bean
    @JobScope
    public Partitioner hpanRecoveryPartitioner() throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(hpanDirectoryPath));
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
    public Partitioner hpanStoreRecoveryPartitioner() throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(hpanWorkerDirectoryPath));
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
    public Step hpanStoreRecoveryMasterStep(HpanStoreService hpanStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("hpan-store-recovery-master-step")
                .partitioner(hpanStoreRecoveryWorkerStep(hpanStoreService, writerTrackerService))
                .partitioner("partition", hpanStoreRecoveryPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor())
                .listener(hpanReaderMasterStepListener(hpanStoreService, writerTrackerService))
                .build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step hpanStoreRecoveryWorkerStep(HpanStoreService hpanStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("hpan-store-recovery-worker-step")
                .<String, String>chunk(chunkSize)
                .reader(hpanItemReader(null))
                .writer(hpanStoreItemWriter(hpanStoreService))
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
    public Step hpanRecoveryMasterStep(HpanStoreService hpanStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("hpan-recovery-master-step")
                .partitioner(hpanRecoveryWorkerStep(hpanStoreService, writerTrackerService))
                .partitioner("partition", hpanRecoveryPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor())
                .listener(hpanReaderMasterStepListener(hpanStoreService, writerTrackerService))
                .build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step hpanRecoveryWorkerStep(HpanStoreService hpanStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("hpan-recovery-worker-step")
                .<String, String>chunk(chunkSize)
                .reader(hpanItemReader(null))
                .writer(hpanItemWriter(hpanStoreService, writerTrackerService))
                .faultTolerant()
                .skipLimit(skipLimit)
                .noSkip(FileNotFoundException.class)
                .skip(Exception.class)
                .taskExecutor(batchConfig.readerTaskExecutor())
                .build();
    }

    @Bean
    public HpanReaderMasterStepListener hpanReaderMasterStepListener(
            HpanStoreService hpanStoreService, WriterTrackerService writerTrackerService) {
        HpanReaderMasterStepListener hpanReaderStepListener = new HpanReaderMasterStepListener();
        hpanReaderStepListener.setHpanStoreService(hpanStoreService);
        hpanReaderStepListener.setWriterTrackerService(writerTrackerService);
        return hpanReaderStepListener;
    }

    /**
     *
     * @return bean configured for usage for chunk reading of a single file
     */
    @Bean
    public Executor writerExecutor() {
        if (this.executorService == null) {
            executorService =  Executors.newFixedThreadPool(executorPoolSize);
        }
        return executorService;
    }



}
