package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import java.io.IOException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;


/**
 * TODO
 */
@Data
@Slf4j
public class PreventReprocessingFilenameAlreadySeenTasklet implements Tasklet {

    private StoreService storeService;
    private TransactionWriterService transactionWriterService;

    /**
     * TODO
     *
     * @param stepContribution
     * @param chunkContext
     * @return the {@link Tasklet} execution status
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
        throws IOException {
        if (this.transactionWriterService.existFileChannel(storeService.getTargetInputFile())) {
            throw new IOException();
        } else {
            return RepeatStatus.FINISHED;
        }
    }

}
