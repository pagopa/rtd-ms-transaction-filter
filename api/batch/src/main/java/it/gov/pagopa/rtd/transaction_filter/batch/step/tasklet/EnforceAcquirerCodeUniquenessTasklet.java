package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerCode;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerCodeFlyweight;
import java.io.IOException;
import java.util.Collection;
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
public class EnforceAcquirerCodeUniquenessTasklet implements Tasklet {

    private StoreService storeService;

    /**
     * TODO
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
        throws IOException {
        // TODO: check that flyweight caches are empties at the end of each execution!
        if (AcquirerCodeFlyweight.cacheSize() != 1) {
            throw new IOException("Acquirer code is not unique within file content (n of distinct values: " + AcquirerCodeFlyweight.cacheSize() + ")");
        } else {
            Collection<AcquirerCode> acquirerCodes = AcquirerCodeFlyweight.values();
            AcquirerCode code = acquirerCodes.iterator().next();
            if (!code.getCode().equals(storeService.getTargetInputFileAbiPart())) {
                throw new IOException("Acquirer code is not unique between file content and file name");
            } else {
                return RepeatStatus.FINISHED;
            }
        }
    }

}
