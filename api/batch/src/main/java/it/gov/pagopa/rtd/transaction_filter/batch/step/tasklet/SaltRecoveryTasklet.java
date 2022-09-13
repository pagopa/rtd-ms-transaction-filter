package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import lombok.Data;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;


/**
 * implementation of the {@link Tasklet}, recovers the salt from a REST service, when enabled
 */
@Data
public class SaltRecoveryTasklet implements Tasklet {

    private HpanConnectorService hpanConnectorService;
    private StoreService storeService;
    private Boolean taskletEnabled = false;

    /**
     * Recovers a string containing the salt to be applied for the pan hashing.
     *
     * @param stepContribution
     * @param chunkContext
     * @return the {@link Tasklet} execution status
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
        if (Boolean.TRUE.equals(taskletEnabled)) {
            storeService.storeSalt(hpanConnectorService.getSalt());
        }
        return RepeatStatus.FINISHED;
    }

}
