package it.gov.pagopa.rtd.transaction_filter.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;

@Slf4j
@EnableScheduling
@RequiredArgsConstructor
@Configuration
@ConditionalOnProperty(value = "spring.batch.job.scheduled", havingValue = "true")
public class TransactionFilterScheduler {

    private final TransactionFilterBatch transactionFilterBatch;

    /**
     * Scheduled method used to launch the configured batch job for processing transaction from a defined directory.
     * The scheduler is based on a cron execution, based on the provided configuration
     * @throws Exception
     */
    @Scheduled(cron = "${batchConfiguration.TransactionFilterBatch.cron}")
    public void launchJob() throws Exception {

        // FIXME: does the scheduler guarantee that only one jobs is running at
        //        any moment in time? if that's not the case, we may have a new
        //        launched while the previous one is still running
        Date startDate = new Date();
        log.info("CsvTransactionReader scheduled job started at {}", startDate);

        transactionFilterBatch.executeBatchJob(startDate);

        Date endDate = new Date();

        log.info("CsvTransactionReader scheduled job ended at {}", endDate);
        log.info("Completed in: {} (ms)", (endDate.getTime() - startDate.getTime()));

    }

}
