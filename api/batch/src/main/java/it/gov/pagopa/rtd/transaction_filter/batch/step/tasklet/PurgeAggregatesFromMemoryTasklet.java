package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import lombok.Data;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;


/**
 * Tasklet responsible for clearing aggregates data from memory.
 */
@Data
public class PurgeAggregatesFromMemoryTasklet implements Tasklet {

    private StoreService storeService;

    /**
     * Clears the aggregations' data from the shared storage.
     *
     * @param stepContribution
     * @param chunkContext
     * @return the {@link Tasklet} execution status
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
        storeService.clearAggregates();
        return RepeatStatus.FINISHED;
    }

}
