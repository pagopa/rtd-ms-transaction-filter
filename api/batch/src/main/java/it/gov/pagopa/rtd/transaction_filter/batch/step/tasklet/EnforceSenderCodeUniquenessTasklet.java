package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.store.SenderCode;
import it.gov.pagopa.rtd.transaction_filter.service.store.SenderCodeFlyweight;
import java.io.IOException;
import java.util.Collection;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;


/**
 * Tasklet responsible for Sender Code uniqueness enforcing
 */
@Data
@Slf4j
public class EnforceSenderCodeUniquenessTasklet implements Tasklet {

    private StoreService storeService;

    /**
     * Makes sure that no more than one sender code has been defined in the
     * input file content, and in that case that the code defined in the file
     * is the same as the one defined in the file name itself.
     *
     * @param stepContribution
     * @param chunkContext
     * @return the {@link Tasklet} execution status
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
        throws IOException {
        SenderCodeFlyweight senderCodeFlyweight = storeService.getSenderCodeFlyweight();
        if (senderCodeFlyweight.cacheSize() == 0) {
            throw new IOException("Input file is empty or every rows presents parsing/validation errors. Please check the logs above.");
        }
        if (senderCodeFlyweight.cacheSize() > 1) {
            throw new IOException("Sender code is not unique within file content (n of distinct values: " + senderCodeFlyweight.cacheSize() + ")");
        }

        Collection<SenderCode> senderCodes = senderCodeFlyweight.values();
        SenderCode code = senderCodes.iterator().next();
        if (!code.getCode().equals(storeService.getTargetInputFileAbiPart())) {
            throw new IOException("Sender code is not unique between file content and file name");
        }

        return RepeatStatus.FINISHED;
    }

}
