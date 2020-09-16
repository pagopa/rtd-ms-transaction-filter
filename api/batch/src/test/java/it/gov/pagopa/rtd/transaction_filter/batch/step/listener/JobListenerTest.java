package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;

import java.util.ArrayList;
import java.util.List;

public class JobListenerTest {

    @Test
    public void afterJobTest_WithFailiure() {

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        execution.setExitStatus(ExitStatus.COMPLETED);
        execution.setStatus(BatchStatus.COMPLETED);

        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = MetaDataInstanceFactory.createStepExecution("A",1L);
        stepExecution1.setExitStatus(ExitStatus.COMPLETED);
        stepExecution1.setStatus(BatchStatus.COMPLETED);
        stepExecutions.add(stepExecution1);

        StepExecution stepExecution2 = MetaDataInstanceFactory.createStepExecution("B", 1L);
        stepExecution2.setExitStatus(ExitStatus.COMPLETED);
        stepExecution2.setStatus(BatchStatus.COMPLETED);
        stepExecutions.add(stepExecution2);

        StepExecution stepExecution3 = MetaDataInstanceFactory.createStepExecution("C", 1L);
        stepExecution3.setExitStatus(ExitStatus.COMPLETED);
        stepExecution3.setStatus(BatchStatus.COMPLETED);
        stepExecutions.add(stepExecution3);

        StepExecution stepExecution4 = MetaDataInstanceFactory.createStepExecution("D", 1L);
        stepExecution4.setExitStatus(ExitStatus.FAILED);
        stepExecution4.setStatus(BatchStatus.COMPLETED);
        stepExecutions.add(stepExecution4);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);

        JobListener jobListener = new JobListener();
        jobListener.afterJob(stepContext.getStepExecution().getJobExecution());

        Assert.assertEquals(stepContext.getStepExecution().getJobExecution().getExitStatus(),ExitStatus.FAILED);

    }

}
