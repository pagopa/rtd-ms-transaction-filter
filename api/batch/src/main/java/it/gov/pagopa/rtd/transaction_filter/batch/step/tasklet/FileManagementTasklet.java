package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import java.security.SecureRandom;
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

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
    private String uploadPendingPath;
    private String hpanDirectory;
    private String outputDirectory;
    private String logsDirectory;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /**
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        String assertionMessage = "directory must be set";
        Assert.notNull(resolver.getResources("file:" + successPath + "*.pgp"),
            assertionMessage);
        Assert.notNull(resolver.getResources("file:" + uploadPendingPath + "*.pgp"),
            assertionMessage);
        Assert.notNull(resolver.getResources(hpanDirectory),
            assertionMessage);
    }

    /**
     * Method that contains the logic for file archival, based on the exit status of each step obtained from the
     * ChunkContext that contains a filename key in the {@link ExecutionContext}
     * @param stepContribution
     * @param chunkContext
     * @return Status of the tasklet execution
     * @throws Exception
     */
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        closeAllFileChannels();

        boolean executionWithErrors = false;
        List<String> errorFilenames = new ArrayList<>();
        hpanDirectory = makePathSystemIndependent(hpanDirectory);

        List<String> hpanResources = getHpanFiles();

        Collection<StepExecution> stepExecutions = chunkContext.getStepContext().getStepExecution().getJobExecution()
                .getStepExecutions();

        // Since more steps can process the same input file we must keep track
        // of already processed files to avoid trying to archive/delete twice
        // the same one (and thus failing the second time).
        List<String> alreadyProcessedFiles = new ArrayList<>();

        for (StepExecution stepExecution : stepExecutions) {
            if (stepExecution.getExecutionContext().containsKey("fileName")) {

                String file = stepExecution.getExecutionContext().getString("fileName");

                if(alreadyProcessedFiles.contains(file)) {
                    log.info("Already managed file: {}", file);
                    continue;
                } else {
                    alreadyProcessedFiles.add(file);
                }

                String path = null;

                try {
                    path = resolver.getResource(file).getFile().getAbsolutePath();
                } catch (Exception e) {
                    log.error(e.getMessage(),e);
                    path = file.replace("file:/", "");
                }

                try {
                    boolean isComplete = BatchStatus.COMPLETED.equals(stepExecution.getStatus()) &&
                            stepExecution.getFailureExceptions().isEmpty();
                    executionWithErrors = executionWithErrors || !isComplete;
                    if (!isComplete) {
                        String[] filename = makePathSystemIndependent(file).split("/");
                        ArrayList<String> filePartsArray = new ArrayList<>(Arrays.asList(
                                filename[filename.length - 1].split("\\.")));
                        if (filePartsArray.size() == 1) {
                            errorFilenames.add(filePartsArray.get(0));
                        } else {
                            filePartsArray.remove(filePartsArray.size()-1);
                            String[] fileParts = new String[0];
                            fileParts = filePartsArray.toArray(fileParts);
                            errorFilenames.add(String.join(".", fileParts));
                        }
                    }

                    boolean isHpanFile = hpanResources.contains(makePathSystemIndependent(path));
                    boolean isOutputFile = path.contains(getOutputDirectoryAbsolutePath());
                    if (Boolean.TRUE.equals(deleteProcessedFiles) || (isComplete && isHpanFile && manageHpanOnSuccess.equals("DELETE"))) {
                        log.info("Removing processed file: {}", file);
                        FileUtils.forceDelete(FileUtils.getFile(path));
                    } else if (!isOutputFile && (!isHpanFile || !isComplete || manageHpanOnSuccess.equals("ARCHIVE"))) {
                        log.info("Archiving processed file: {}", file);
                        archiveFile(file, path, isComplete);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }

            }
        }

        if ("ALWAYS".equals(deleteOutputFiles) || ("ERROR".equals(deleteOutputFiles) && executionWithErrors)) {
            List<Resource> outputDirectoryResources =
                    Arrays.asList(resolver.getResources(makePathSystemIndependent(outputDirectory) + "/*"));
            outputDirectoryResources.forEach(outputDirectoryResource ->
            {
                if (deleteOutputFiles.equals("ALWAYS") || (errorFilenames.stream().anyMatch(
                        errorFilename -> {
                            try {
                                return outputDirectoryResource.getFile().getAbsolutePath().contains(errorFilename);
                            } catch (IOException e) {
                                log.error(e.getMessage(),e);
                                return false;
                            }
                        }))
                ) {
                    try {
                        log.info("Deleting output file: {}", outputDirectoryResource.getFile());
                        FileUtils.forceDelete(outputDirectoryResource.getFile());
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            });
        }

        deleteEmptyLogFiles();

        return RepeatStatus.FINISHED;
    }

    @SneakyThrows
    private String getOutputDirectoryAbsolutePath() {
        return Arrays.stream(resolver.getResources(outputDirectory)).map(resource -> {
            try {
                return makePathSystemIndependent(resource.getFile().getAbsolutePath());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return "";
            }
        }).findAny().orElse(null);
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

    String makePathSystemIndependent(String path) {
        return path.replace("\\", "/");
    }

    private void closeAllFileChannels() {
        if (transactionWriterService != null) {
            transactionWriterService.closeAll();
        }
    }

    @SneakyThrows
    private void archiveFile(String file, String path, boolean isCompleted) {
        File destinationFile = getDestionationFileByStatus(file, isCompleted);
        FileUtils.moveFile(FileUtils.getFile(path), destinationFile);
    }

    @SneakyThrows
    private File getDestionationFileByStatus(String sourceFilePath, boolean isCompleted) {

        sourceFilePath = makePathSystemIndependent(sourceFilePath);
        String[] pathSplitted = sourceFilePath.split("/");
        String filename = pathSplitted[pathSplitted.length - 1];
        String destinationPath;
        if (isCompleted) {
            String archivalPath = resolver.getResources(successPath)[0].getFile().getAbsolutePath();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
            destinationPath = archivalPath + File.separator + SecureRandom.getInstanceStrong().nextLong(Long.MAX_VALUE) +
                "_" + OffsetDateTime.now().format(fmt) + "_" + filename;
        } else {
            String archivalPath = resolver.getResources(uploadPendingPath)[0].getFile().getAbsolutePath();
            destinationPath = archivalPath + File.separator + filename;
        }

        return FileUtils.getFile(destinationPath);
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
