package it.gov.pagopa.rtd.transaction_filter.batch.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@Data
@PropertySource("classpath:config/transactionFilterBatch.properties")
public class BatchConfig {

    @Value("${batchConfiguration.TransactionFilterBatch.partitionerMaxPoolSize}")
    public Integer partitionerMaxPoolSize;
    @Value("${batchConfiguration.TransactionFilterBatch.partitionerCorePoolSize}")
    public Integer partitionerCorePoolSize;
    @Value("${batchConfiguration.TransactionFilterBatch.readerMaxPoolSize}")
    public Integer readerMaxPoolSize;
    @Value("${batchConfiguration.TransactionFilterBatch.readerCorePoolSize}")
    public Integer readerCorePoolSize;

    /**
     *
     * @return bean configured for usage in the partitioner instance of the job
     */
    @Bean
    public TaskExecutor partitionerTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(partitionerMaxPoolSize);
        taskExecutor.setCorePoolSize(partitionerCorePoolSize);
        taskExecutor.afterPropertiesSet();
        return taskExecutor;
    }

    /**
     *
     * @return bean configured for usage for chunk reading of a single file
     */
    @Bean
    public TaskExecutor readerTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(readerMaxPoolSize);
        taskExecutor.setCorePoolSize(readerCorePoolSize);
        taskExecutor.afterPropertiesSet();
        return taskExecutor;
    }

}
