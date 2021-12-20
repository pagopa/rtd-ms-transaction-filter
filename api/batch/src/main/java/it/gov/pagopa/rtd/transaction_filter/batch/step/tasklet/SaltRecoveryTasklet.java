package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import lombok.Data;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;


/**
 * implementation of the {@link Tasklet}, recovers the salt from a REST service,
 * when enabled
 */

@Data
public class SaltRecoveryTasklet implements Tasklet {

    private HpanConnectorService hpanConnectorService;
    private HpanStoreService hpanStoreService;
    private Boolean taskletEnabled = false;

    /**
     * Recovers a string containing the salt to be applied for the pan hashing
     * @param stepContribution
     * @param chunkContext
     * @return
     * @throws Exception
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        if (taskletEnabled) {
            hpanStoreService.storeSalt(hpanConnectorService.getSalt());
        }
        return RepeatStatus.FINISHED;
    }

}
