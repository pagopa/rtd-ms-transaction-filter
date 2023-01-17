package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.batch.model.DeleteOutputFilesEnum;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;

/**
 * implementation of the {@link Tasklet}, in which the execute method contains the logic for processed file archival,
 * based on the status of conclusion for every processed file
 */

@Data
@Slf4j
public class FileManagementTasklet implements Tasklet, InitializingBean {

    private TransactionWriterService transactionWriterService;
    private Boolean deleteProcessedFiles;
    private String deleteOutputFiles;
    private String manageHpanOnSuccess;
    private String successPath;
    private String errorPath;
    private String uploadPendingPath;
    private String hpanDirectory;
    private String outputDirectory;
    private String logsDirectory;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private static final String PGP_REGEX = "*.pgp";
    private static final String FILE_PROTOCOL = "file:";

    @Override
    public void afterPropertiesSet() throws Exception {
        String assertionMessage = "directory must be set";
        Assert.notNull(resolver.getResources(FILE_PROTOCOL + successPath + PGP_REGEX),
            assertionMessage);
        Assert.notNull(resolver.getResources(FILE_PROTOCOL + errorPath + PGP_REGEX),
            assertionMessage);
        Assert.notNull(resolver.getResources(FILE_PROTOCOL + uploadPendingPath + PGP_REGEX),
            assertionMessage);
        Assert.notNull(resolver.getResources(hpanDirectory),
            assertionMessage);
    }

    /**
     * Method that contains the logic for file archival, based on the exit status of each step obtained from the
     * ChunkContext that contains a filename key in the {@link ExecutionContext}
     *
     * recap of every step with relative filename evaluated:
     *          step_name                           - file
     * transaction-checksum-worker-step             - input file
     * transaction-aggregation-reader-worker-step   - input file
     * transaction-aggregation-writer-worker-step   - input file
     * encrypt-aggregate-chunks-worker-step         - output csv file ade
     * transaction-sender-ade-worker-step           - output pgp file ade
     * hpan-recovery-worker-step                    - hpan file
     * transaction-filter-worker-step               - input file
     * transaction-sender-rtd-worker-step           - output pgp file rtd
     *
     * @return Status of the tasklet execution
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {

        closeAllFileChannels();

        boolean executionWithErrors = false;
        List<String> errorFilenames = new ArrayList<>();
        hpanDirectory = makePathSystemIndependent(hpanDirectory);

        Collection<StepExecution> stepExecutions = chunkContext.getStepContext().getStepExecution().getJobExecution()
            .getStepExecutions();

        // map to keep track of the worst status associated to a filename among all steps with the same filename
        Map<String, BatchStatus> filenameWithStatusMap = new HashMap<>();

        for (StepExecution stepExecution : stepExecutions) {
            if (stepExecution.getExecutionContext().containsKey("fileName")) {

                String file = stepExecution.getExecutionContext().getString("fileName");

                boolean isComplete = BatchStatus.COMPLETED.equals(stepExecution.getStatus()) &&
                    stepExecution.getFailureExceptions().isEmpty();
                executionWithErrors = executionWithErrors || !isComplete;
                // errorFilenames is populated with the filename of steps that went in error.
                // this list will be compared with the files in the output directory to delete those which matches
                if (!isComplete) {
                    errorFilenames.add(getFilenameWithoutExtension(file));
                }

                filenameWithStatusMap.merge(file, stepExecution.getStatus(),
                    (oldValue, newValue) -> BatchStatus.FAILED.equals(oldValue)? oldValue : newValue);
            }
        }

        // evaluate only the worst case status among steps with the same filename (e.g. checksum and transaction processing steps)
        manageFilesBasedOnFilenameAndStatus(filenameWithStatusMap);

        // this code removes only the RTD files because the input filename matches the output filename (without extensions)
        // in order to maintain the retro compatibility with the RTD files, this code will stay until the splitting on RTD is implemented
        deleteOutputFilesRtdBasedOnFlags(executionWithErrors, errorFilenames);

        deleteEmptyLogFiles();

        return RepeatStatus.FINISHED;
    }

    private void closeAllFileChannels() {
        if (transactionWriterService != null) {
            transactionWriterService.closeAll();
        }
    }

    @SneakyThrows
    private List<String> getHpanFiles() {
        return Arrays.stream(resolver.getResources(hpanDirectory)).map(resource -> {
            try {
                return makePathSystemIndependent(resource.getFile().getAbsolutePath());
            } catch (IOException e) {
                log.error(e.getMessage(),e);
                return null;
            }
        }).collect(Collectors.toList());
    }

    private String makePathSystemIndependent(String path) {
        return path.replace("\\", "/");
    }

    private void manageFilesBasedOnFilenameAndStatus(Map<String, BatchStatus> filenameWithStatusMap) {
        List<String> hpanResources = getHpanFiles();
        filenameWithStatusMap.forEach((file, status) -> {
            String absolutePath = getAbsolutePathFromFile(file);
            boolean isComplete = BatchStatus.COMPLETED.equals(status);

            // output file
            boolean isOutputFile = isFileInsideOutputDirectory(absolutePath);
            if (isOutputFile) {
                manageOutputFile(absolutePath, isComplete);
                return;
                // the csv output files are not handled here, Should they? No, they must be deleted or left in output folder
            }

            // pending file
            boolean isPendingFile = isFileInsidePendingDirectory(absolutePath);
            if (isPendingFile) {
                managePendingFiles(absolutePath, isComplete);
                return;
            }

            boolean isHpanFile = hpanResources.contains(makePathSystemIndependent(absolutePath));
            if (Boolean.TRUE.equals(deleteProcessedFiles)) {
                deleteFile(new File(absolutePath));
            } else if (isHpanFile) {
                manageHpanFiles(file, absolutePath, isComplete);
            } else {
                // handle input file archive
                archiveFile(file, absolutePath, isComplete);
            }
        });
    }

    private String getAbsolutePathFromFile(String file) {
        String path;

        try {
            path = resolver.getResource(file).getFile().getAbsolutePath();
        } catch (IOException e) {
            log.warn("file {} not found", file);
            path = file.replace("file:/", "");
        }
        return path;
    }

    private boolean isFileInsideOutputDirectory(String absolutePath) {
        String pathWithoutFile = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
        return pathWithoutFile.equals(getAbsolutePathFromFile(outputDirectory));
    }

    private void manageOutputFile(String path, boolean isComplete) {
        // move every pgp file that failed into pending folder
        if (isOutputFileToMoveToPending(path, isComplete)) {
            moveToPendingDirectory(path);
        } else if (isOutputFileToDelete(isComplete)) {
            deleteFile(FileUtils.getFile(path));
        }
    }

    private boolean isFileInsidePendingDirectory(String absolutePath) {
        String pathWithoutFile = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
        return pathWithoutFile.equals(getAbsolutePathFromFile(uploadPendingPath));
    }

    private void managePendingFiles(String absolutePath, boolean isComplete) {
        // delete the pending files if they have been sent, otherwise leave them in directory pending
        if (isComplete) {
            deleteFile(new File(absolutePath));
        }
    }

    private boolean isOutputFileToMoveToPending(String path, boolean isComplete) {
        return path.endsWith(".pgp") && !isComplete;
    }

    @SneakyThrows
    private void moveToPendingDirectory(String path) {
        log.info("Moving to pending directory {}", path);
        String archivalPath = resolver.getResources(uploadPendingPath)[0].getFile().getAbsolutePath();
        File destinationFile = FileUtils.getFile(archivalPath + File.separator + getFilenameFromPath(path));
        FileUtils.moveFile(FileUtils.getFile(path), destinationFile);
    }

    private boolean isOutputFileToDelete(boolean isComplete) {
        return DeleteOutputFilesEnum.ALWAYS.name().equals(deleteOutputFiles) ||
            (DeleteOutputFilesEnum.ERROR.name().equals(deleteOutputFiles) && !isComplete) ||
            Boolean.TRUE.equals(deleteProcessedFiles);
    }

    @SneakyThrows
    private void deleteFile(File file) {
        log.info("Deleting file: {}", file);
        FileUtils.deleteQuietly(file) ;
    }

    @SneakyThrows
    private void deleteOutputFilesRtdBasedOnFlags(boolean executionWithErrors, List<String> errorFilenames) {
        if ("ALWAYS".equals(deleteOutputFiles) || ("ERROR".equals(deleteOutputFiles) && executionWithErrors)) {
            Arrays.stream(resolver.getResources(makePathSystemIndependent(outputDirectory) + "/*"))
                .map(this::getFileFromResource)
                .filter(File::isFile)
                .filter(outputFilename -> "ALWAYS".equals(deleteOutputFiles) || errorFilenames.contains(getFilenameWithoutExtension(outputFilename)))
                .forEach(this::deleteFile);
        }
    }

    @SneakyThrows
    private File getFileFromResource(Resource outputDirectoryResource) {
        return outputDirectoryResource.getFile();
    }

    private String getFilenameWithoutExtension(File outputFilename) {
        return getFilenameWithoutExtension(outputFilename.getAbsolutePath());
    }

    private String getFilenameWithoutExtension(String file) {
        String filename = getFilenameFromPath(file);
        return filename.substring(0, filename.lastIndexOf("."));
    }

    private void manageHpanFiles(String file, String path, boolean isComplete) {
        if (isComplete && "DELETE".equals(manageHpanOnSuccess)) {
            deleteFile(new File(path));
        } else if (!isComplete || "ARCHIVE".equals(manageHpanOnSuccess)) {
            archiveFile(file, path, isComplete);
        }
    }

    private void archiveFile(String file, String path, boolean isCompleted) {
        log.info("Archiving processed file: {}", file);
        try {
            File destinationFile = getDestinationFileByStatus(file, isCompleted);
            FileUtils.moveFile(FileUtils.getFile(path), destinationFile);
        } catch (IOException e) {
            log.error("File {} cannot be moved to the destination path. Reason: {}", path, e.getMessage());
        }
    }

    @SneakyThrows
    private File getDestinationFileByStatus(String sourceFilePath, boolean isCompleted) {

        String filename = getFilenameFromPath(sourceFilePath);
        String destinationPath;
        if (isCompleted) {
            String archivalPath = resolver.getResources(successPath)[0].getFile().getAbsolutePath();
            destinationPath = archivalPath + File.separator + addRandomPrefixToFilename(filename);
        } else {
            String archivalPath = resolver.getResources(errorPath)[0].getFile().getAbsolutePath();
            destinationPath = archivalPath + File.separator + addRandomPrefixToFilename(filename);
        }

        return FileUtils.getFile(destinationPath);
    }

    private String getFilenameFromPath(String path) {
        String[] pathSplitted = makePathSystemIndependent(path).split("/");
        return pathSplitted[pathSplitted.length - 1];
    }

    private String addRandomPrefixToFilename(String filename) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        return UUID.randomUUID().toString().replace("-", "").substring(0, 20) +
            "_" + OffsetDateTime.now().format(fmt) + "_" + filename;
    }

    @SneakyThrows
    private void deleteEmptyLogFiles() {
        if (logsDirectory == null) {
            return;
        }

        FileUtils.listFiles(
                resolver.getResources(logsDirectory)[0].getFile(), new String[]{"csv"},false)
            .stream()
            .filter(file -> FileUtils.sizeOf(file) == 0)
            .forEach(file -> {
                log.debug("Removing empty log file: {}", file.getName());
                FileUtils.deleteQuietly(file);
            });
    }
}