package it.gov.pagopa.rtd.transaction_filter.batch.step;

import it.gov.pagopa.rtd.transaction_filter.batch.config.BatchConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.TokenPanReaderMasterStepListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.PGPFlatFileItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.TokenPanStoreWriter;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.TokenPanWriter;
import it.gov.pagopa.rtd.transaction_filter.service.TokenPanStoreService;
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
@PropertySource("classpath:config/tokenPanReaderStep.properties")
public class TokenPanReaderStep {

    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanList.partitionerSize}")
    private Integer partitionerSize;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanList.chunkSize}")
    private Integer chunkSize;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanList.skipLimit}")
    private Integer skipLimit;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanList.tokenPanDirectoryPath}")
    private String tokenPanDirectoryPath;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanList.tokenPanWorkerDirectoryPath}")
    private String tokenPanWorkerDirectoryPath;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanList.secretKeyPath}")
    private String secretKeyPath;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanList.passphrase}")
    private String passphrase;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanList.applyDecrypt}")
    private Boolean applyDecrypt;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanList.applyHashing}")
    private Boolean applyPanListHashing;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanList.poolSize}")
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
    public PGPFlatFileItemReader enrolledTokenPanItemReader(
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
    public TokenPanWriter enrolledTokenPanItemWriter(
            TokenPanStoreService tokenPanStoreService, WriterTrackerService writerTrackerService) {
        TokenPanWriter tokenPanWriter = new TokenPanWriter(tokenPanStoreService, writerTrackerService);
        tokenPanWriter.setExecutor(writerExecutor());
        return tokenPanWriter;
    }

    /**
     *
     * @return instance of the itemWriter to be used in the first step of the configured job
     */
    @Bean
    @StepScope
    public TokenPanStoreWriter enrolledTokenPanStoreItemWriter(TokenPanStoreService tokenPanStoreService) {
        return new TokenPanStoreWriter(tokenPanStoreService);
    }

    /**
     *
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws Exception
     */
    @Bean
    @JobScope
    public Partitioner enrolledTokenPanRecoveryPartitioner() throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(tokenPanDirectoryPath));
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
    public Partitioner enrolledTokenPanStoreRecoveryPartitioner() throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(tokenPanWorkerDirectoryPath));
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
    public Step enrolledTokenPanStoreRecoveryMasterStep(TokenPanStoreService tokenPanStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("token-pan-store-recovery-master-step")
                .partitioner(enrolledTokenPanStoreRecoveryWorkerStep(tokenPanStoreService, writerTrackerService))
                .partitioner("partition", enrolledTokenPanStoreRecoveryPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor())
                .listener(enrolledTokenPanReaderMasterStepListener(tokenPanStoreService, writerTrackerService))
                .build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step enrolledTokenPanStoreRecoveryWorkerStep(TokenPanStoreService tokenPanStoreService,
                                                WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("token-pan-store-recovery-worker-step")
                .<String, String>chunk(chunkSize)
                .reader(enrolledTokenPanItemReader(null))
                .writer(enrolledTokenPanStoreItemWriter(tokenPanStoreService))
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
    public Step enrolledTokenPanRecoveryMasterStep(TokenPanStoreService tokenPanStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("token-pan-recovery-master-step")
                .partitioner(enrolledTokenPanRecoveryWorkerStep(tokenPanStoreService, writerTrackerService))
                .partitioner("partition", enrolledTokenPanRecoveryPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor())
                .listener(enrolledTokenPanReaderMasterStepListener(tokenPanStoreService, writerTrackerService))
                .build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step enrolledTokenPanRecoveryWorkerStep(TokenPanStoreService tokenPanStoreService,
                                       WriterTrackerService writerTrackerService) throws Exception {
        return stepBuilderFactory.get("token-recovery-worker-step")
                .<String, String>chunk(chunkSize)
                .reader(enrolledTokenPanItemReader(null))
                .writer(enrolledTokenPanItemWriter(tokenPanStoreService, writerTrackerService))
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
    public TokenPanReaderMasterStepListener enrolledTokenPanReaderMasterStepListener(
            TokenPanStoreService tokenPanStoreService, WriterTrackerService writerTrackerService) {
        TokenPanReaderMasterStepListener tokenPanReaderMasterStepListener = new TokenPanReaderMasterStepListener();
        tokenPanReaderMasterStepListener.setTokenPanStoreService(tokenPanStoreService);
        tokenPanReaderMasterStepListener.setWriterTrackerService(writerTrackerService);
        return tokenPanReaderMasterStepListener;
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
