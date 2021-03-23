package it.gov.pagopa.rtd.transaction_filter.batch.step;

import it.gov.pagopa.rtd.transaction_filter.batch.config.BatchConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.PGPFlatFileItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.HpanWriter;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
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

import javax.validation.ConstraintViolationException;
import java.io.FileNotFoundException;

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
    private final StepBuilderFactory stepBuilderFactory;

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
    public HpanWriter hpanItemWriter(HpanStoreService hpanStoreService) {
        return new HpanWriter(hpanStoreService, this.applyPanListHashing);
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
     * @return master step to be used as the formal main step in the reading phase of the job,
     * partitioned for scalability on multiple file reading
     * @throws Exception
     */
    @Bean
    public Step hpanRecoveryMasterStep(HpanStoreService hpanStoreService) throws Exception {
        return stepBuilderFactory.get("hpan-recovery-master-step").partitioner(hpanRecoveryWorkerStep(hpanStoreService))
                .partitioner("partition", hpanRecoveryPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step hpanRecoveryWorkerStep(HpanStoreService hpanStoreService) throws Exception {
        return stepBuilderFactory.get("hpan-recovery-worker-step")
                .<String, String>chunk(chunkSize)
                .reader(hpanItemReader(null))
                .writer(hpanItemWriter(hpanStoreService))
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
