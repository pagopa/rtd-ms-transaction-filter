package it.gov.pagopa.rtd.transaction_filter.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

@Slf4j
public class TransactionReaderStepListener implements StepExecutionListener {


    @Override
    public void beforeStep(StepExecution stepExecution) {
        if (log.isInfoEnabled()) {
            log.info("Starting processing for file: " + stepExecution.getExecutionContext().get("fileName"));
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (log.isInfoEnabled()) {
            log.info("Processing for file: " + stepExecution.getExecutionContext().get("fileName") +
                    " ended with status: " + stepExecution.getExitStatus() );
        }
        return stepExecution.getExitStatus();
    }

}
