package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class JobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {}

    @Override
    public void afterJob(JobExecution jobExecution) {
        boolean hasErrors = jobExecution.getStepExecutions().stream().anyMatch(stepExecution ->
                (!ExitStatus.COMPLETED.equals(stepExecution.getExitStatus()) &&
               !stepExecution.getExitStatus().equals(new ExitStatus("COMPLETED WITH SKIPS"))) ||
               !BatchStatus.COMPLETED.equals(stepExecution.getStatus()) ||
                stepExecution.getFailureExceptions().size() > 0);
        if (hasErrors) {
            jobExecution.setExitStatus(ExitStatus.FAILED);
        }
    }
}
