package it.gov.pagopa.rtd.transaction_filter.batch.step;

import it.gov.pagopa.rtd.transaction_filter.batch.config.BatchConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.PGPFlatFileItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.HpanWriter;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.io.FileNotFoundException;
import java.io.IOException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

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
    @Value("${batchConfiguration.TransactionFilterBatch.panList.secretKeyPath}")
    private String secretKeyPath;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.passphrase}")
    private String passphrase;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.applyDecrypt}")
    private Boolean applyDecrypt;
    @Value("${batchConfiguration.TransactionFilterBatch.panList.applyHashing}")
    private Boolean applyPanListHashing;

    private final BatchConfig batchConfig;

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
    public HpanWriter hpanItemWriter(StoreService storeService) {
        return new HpanWriter(storeService, this.applyPanListHashing);
    }

    /**
     *
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws Exception
     */
    @Bean
    @JobScope
    public Partitioner hpanRecoveryPartitioner() throws IOException {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(hpanDirectoryPath));
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     *
     * @return master step to be used as the formal main step in the reading phase of the job,
     * partitioned for scalability on multiple file reading
     */
    @Bean
    public Step hpanRecoveryMasterStep(JobRepository jobRepository,
        Step hpanRecoveryWorkerStep,
        Partitioner hpanRecoveryPartitioner
    ) {
        return new StepBuilder("hpan-recovery-master-step", jobRepository)
                .partitioner(hpanRecoveryWorkerStep)
                .partitioner("partition", hpanRecoveryPartitioner)
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     */
    @Bean
    public Step hpanRecoveryWorkerStep(JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        StoreService storeService
    ) {
        return new StepBuilder("hpan-recovery-worker-step", jobRepository)
                .<String, String>chunk(chunkSize, transactionManager)
                .reader(hpanItemReader(null))
                .writer(hpanItemWriter(storeService))
                .faultTolerant()
                .skipLimit(skipLimit)
                .noSkip(FileNotFoundException.class)
                .skip(Exception.class)
                .noRetry(Exception.class)
                .noRollback(Exception.class)
                .taskExecutor(batchConfig.readerTaskExecutor())
                .build();
    }

}
