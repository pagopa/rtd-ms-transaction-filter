package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import lombok.Data;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;


/**
 * {@link Tasklet} implementations responsible for the retrieval of PGP public keys
 * exposed via authenticated PagoPA webservices.
 */
@Data
public class PagopaPublicKeyRecoveryTasklet implements Tasklet {

    private HpanConnectorService hpanConnectorService;
    private StoreService storeService;
    private boolean taskletEnabled = false;

    /**
     * Recovers the PagoPA public key via remote endpoint.
     *
     * @param stepContribution
     * @param chunkContext
     * @return the {@link Tasklet} execution status
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
        if (taskletEnabled) {
            this.storeService.storeKey("pagopa", hpanConnectorService.getPublicKey());
        }
        return RepeatStatus.FINISHED;
    }

}
