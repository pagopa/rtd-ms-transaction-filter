package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

/**
 * Implementation of {@link StepExecutionListener}, to be used to log and define the exit status of a step
 */

@Slf4j
@Data
public class TransactionReaderStepListener implements StepExecutionListener {

    private TransactionWriterService transactionWriterService;
    private String errorTransactionsLogsPath;
    private String executionDate;
    private String prefix;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String filename = String.valueOf(stepExecution.getExecutionContext().get("fileName"));
        log.info("Starting processing for file: {}", filename);
        String file = filename.replaceAll("\\\\", "/");
        String[] fileArr = file.split("/");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            transactionWriterService.openFileChannel(
                    resolver.getResource(errorTransactionsLogsPath).getFile().getAbsolutePath()
                            .concat("/".concat(executionDate))
                    + "_" + prefix + "_FilteredRecords_"+fileArr[fileArr.length-1]);
            transactionWriterService.openFileChannel(
                    resolver.getResource(errorTransactionsLogsPath).getFile().getAbsolutePath()
                            .concat("/".concat(executionDate))
                            + "_" + prefix + "_ErrorRecords_"+fileArr[fileArr.length-1]);
        } catch (IOException e) {
            log.error(e.getMessage(),e);
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        ExitStatus exitStatus = stepExecution.getExitStatus();

        if (!exitStatus.getExitCode().equals(ExitStatus.FAILED.getExitCode()) &&
                stepExecution.getSkipCount() > 0) {
            exitStatus = new ExitStatus("COMPLETED WITH SKIPS");
        }

        log.info("Processing for file: {} ended with status: {}",
                stepExecution.getExecutionContext().get("fileName"), exitStatus.getExitCode());

        return exitStatus;

    }

}
