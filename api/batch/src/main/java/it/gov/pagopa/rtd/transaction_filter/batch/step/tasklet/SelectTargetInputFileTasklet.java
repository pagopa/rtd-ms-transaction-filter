package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep;
import it.gov.pagopa.rtd.transaction_filter.batch.utils.PathResolver;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.io.IOException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;


/**
 * Tasklet responsible for selecting a single input file to process in the current job's execution.
 */
@Data
@Slf4j
public class SelectTargetInputFileTasklet implements Tasklet {

    private StoreService storeService;
    private String transactionDirectoryPath;
    private PathResolver pathResolver;

    /**
     * Scans the input directory and select a target file for current execution.
     *
     * Older files are prioritized over newly ones. The order is lexicographic and is calculated
     * on the substring composed by date, time and progressive. Since the filename has been
     * pre-validated for correctness we can safely assume here that the characters considered here
     * are only numeric, thus the lexicographic order is equivalent to a datetime comparison.
     *
     * @param stepContribution
     * @param chunkContext
     * @return the {@link Tasklet} execution status
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
        throws IOException {
        Resource[] transactionResources = pathResolver.getCsvResources(transactionDirectoryPath);
        transactionResources = TransactionFilterStep.filterValidFilenames(transactionResources);

        // The job is started only if input trx directory contains at least one valid file.
        // The following condition should not verify during normal circumstances but we must
        // check for weird situations (e.g. something emptied the directory between the job start
        // and the execution of this step)
        if (transactionResources.length == 0) {
            throw new IOException("No resources in input trx directory! Quitting job immediately");
        }

        String olderFilename = "";
        String olderDateTimeProgressive = "99999999999999999"; // yyyymmddhhmmssnnn

        for (Resource res : transactionResources) {
            String filename = res.getFilename();
            if (filename == null) {
                log.warn("Resource returned a null filename");
                continue;
            }
            String[] filenameParts = filename.split("\\.");
            String dateTimeProgressive = filenameParts[3] + filenameParts[4] + filenameParts[5];
            if (dateTimeProgressive.compareTo(olderDateTimeProgressive) < 0) {
                olderFilename = res.getFilename();
                olderDateTimeProgressive = dateTimeProgressive;
            }

        }
        log.info("Current job execution will process input file: " + olderFilename);
        storeService.setTargetInputFile(olderFilename);
        return RepeatStatus.FINISHED;
    }

}
