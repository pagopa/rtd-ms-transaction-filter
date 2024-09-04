package it.gov.pagopa.rtd.transaction_filter.batch;


import it.gov.pagopa.rtd.transaction_filter.batch.step.PanReaderStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep;
import it.gov.pagopa.rtd.transaction_filter.batch.utils.PathResolver;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.util.Date;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@Data
public class BatchExecutor {

    private final Job job;
    private final JobLauncher jobLauncher;
    private final TransactionFilterStep transactionFilterStep;
    private final PanReaderStep panReaderStep;
    private final StoreService storeService;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled}")
    private Boolean hpanListRecoveryEnabled;
    private final PathResolver pathResolver;

    /**
     *
     * @return Method to start the execution of the transaction filter job
     * @param startDate starting date for the batch job execution
     */
    @SneakyThrows
    public JobExecution execute(Date startDate) {
        Resource[] transactionResources = pathResolver.getCsvResources(
            transactionFilterStep.getTransactionDirectoryPath());
        transactionResources = TransactionFilterStep.filterValidFilenames(transactionResources);

        String hpanPath = panReaderStep.getHpanDirectoryPath();
        Resource[] hpanResources = pathResolver.getResources(hpanPath);

        JobExecution execution = null;

        /*
          The jobLauncher run method is called only if, based on the configured properties, a matching transaction
          resource is found, and either the remote pan list recovery is enabled, or a pan list file is available locally
          on the configured path
         */
        if (transactionResources.length == 0) {
            log.info("No transaction file has been found on configured path: {}", transactionFilterStep.getTransactionDirectoryPath());
        }

        if (Boolean.FALSE.equals(hpanListRecoveryEnabled) && hpanResources.length == 0) {
            log.info("No hpan file has been found on configured path: {}", hpanPath);
        }

        if (transactionResources.length > 0 &&
                (hpanListRecoveryEnabled || hpanResources.length > 0)) {

            log.info("Found {} {}. Starting filtering process",
                    transactionResources.length, (transactionResources.length > 1 ? "resources" : "resource")
            );

            execution = jobLauncher.run(job,
                    new JobParametersBuilder()
                            .addDate("startDateTime", startDate)
                            .toJobParameters());
            clearStoreService();

        }

        return execution;
    }

    public void clearStoreService() {
        storeService.clearAll();
    }

}