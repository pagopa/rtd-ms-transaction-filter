package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

public class TokenPanReaderStepListenerTest {

    @SneakyThrows
    @Test
    public void afterStepWithSkips() {

        StepExecution stepExecution = new StepExecution("test-step", new JobExecution(1L));
        stepExecution.setStatus(BatchStatus.COMPLETED);
        stepExecution.setProcessSkipCount(1);

        TokenPanReaderStepListener tokenPanReaderStepListener = new TokenPanReaderStepListener();
        ExitStatus status = tokenPanReaderStepListener.afterStep(stepExecution);
        Assert.assertEquals(new ExitStatus("COMPLETED WITH SKIPS"), status);

    }

}