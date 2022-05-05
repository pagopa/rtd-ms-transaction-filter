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
 * {@link Tasklet} implementation checking that the job's target input file
 * hasn't been processed in previous jobs.
 *
 * The program keeps track of previous jobs only in memory, so it cannot
 * prevent the reprocessing of a file that has been processed during a previous
 * execution of the application.
 *
 * Also keep in mind that this tasklet relies exclusively on the filename, and
 * ignores the content of the input files. To prevent that the same identical file
 * but named differently will be processed twice look at the TransactionChecksumTasklet.
 */
@Data
@Slf4j
public class PreventReprocessingFilenameAlreadySeenTasklet implements Tasklet {

    private StoreService storeService;
    private TransactionWriterService transactionWriterService;

    /**
     * Terminates with failure the current job if the filename of the target input file has been
     * already processed in previous jobs.
     *
     * @param stepContribution
     * @param chunkContext
     * @return the {@link Tasklet} execution status
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
        throws IOException {
        if (this.transactionWriterService.existFileChannelForFilename(storeService.getTargetInputFile())) {
            throw new IOException();
        } else {
            return RepeatStatus.FINISHED;
        }
    }

}
