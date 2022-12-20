package it.gov.pagopa.rtd.transaction_filter.batch.step;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import it.gov.pagopa.rtd.transaction_filter.batch.config.BatchConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.PGPFlatFileItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.HpanWriter;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
@Slf4j
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
     * @throws Exception
     */
    @Bean
    public Step hpanRecoveryMasterStep(StoreService storeService) throws IOException {
        return stepBuilderFactory.get("hpan-recovery-master-step").partitioner(hpanRecoveryWorkerStep(storeService))
                .partitioner("partition", hpanRecoveryPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     */
    @Bean
    public Step hpanRecoveryWorkerStep(StoreService storeService) {
        return stepBuilderFactory.get("hpan-recovery-worker-step")
                .<String, String>chunk(chunkSize)
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

  @Bean
  public Step bloomFilterRecoveryStep(StoreService storeService) throws IOException {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] bloomFilterResource = resolver.getResources(hpanDirectoryPath);
      Tasklet tasklet = (stepContribution, chunkContext) -> {
        BloomFilter<String> bloomFilter = BloomFilter.readFrom(
            Files.newInputStream(bloomFilterResource[0].getFile().toPath()),
            Funnels.stringFunnel(StandardCharsets.UTF_8));
        storeService.storeBloomFilter(bloomFilter);
        return RepeatStatus.FINISHED;
      };

    return stepBuilderFactory
          .get("bloom-filter-recovery-step")
          .tasklet(tasklet).build();
  }

}
