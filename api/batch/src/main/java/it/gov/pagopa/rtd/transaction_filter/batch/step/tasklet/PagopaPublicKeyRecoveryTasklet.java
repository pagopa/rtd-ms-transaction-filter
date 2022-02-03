package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import lombok.Data;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;


/**
 * TODO
 */

@Data
public class PagopaPublicKeyRecoveryTasklet implements Tasklet {

    private HpanConnectorService hpanConnectorService;
    private HpanStoreService hpanStoreService;
    private Boolean taskletEnabled = false;

    /**
     * TODO
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        if (taskletEnabled) {
            this.hpanStoreService.storeKey("pagopa", hpanConnectorService.getPublicKey());
        }
        return RepeatStatus.FINISHED;
    }

}
