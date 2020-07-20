package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
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

/**
 * implementation of the {@link Tasklet}, in which the execute method contains the logic for processed file archival,
 * based on the status of conclusion for every file processed
 */

@Data
@Slf4j
public class FileManagementTasklet implements Tasklet, InitializingBean {

    private Boolean deleteProcessedFiles;
    private String deleteOutputFiles;
    private String manageHpanOnSuccess;
    private String successPath;
    private String errorPath;
    private String hpanDirectory;
    private String outputDirectory;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /**
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(resolver.getResources("file:" + successPath + "*.pgp"),
                "directory must be set");
        Assert.notNull(resolver.getResources("file:" + errorPath + "*.pgp"),
                "directory must be set");
        Assert.notNull(resolver.getResources(hpanDirectory),
                "directory must be set");
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

        Boolean executionWithErrors = false;
        List<String> errorFilenames = new ArrayList<>();

        Collection<StepExecution> stepExecutions = chunkContext.getStepContext().getStepExecution().getJobExecution()
                .getStepExecutions();
        for (StepExecution stepExecution : stepExecutions) {
            if (stepExecution.getExecutionContext().containsKey("fileName")) {

                String file = stepExecution.getExecutionContext().getString("fileName");
                String path = null;

                try {
                    path = resolver.getResource(file).getFile().getAbsolutePath();
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error(e.getMessage(),e);
                    }
                    path = file.replace("file:/", "");
                }

                if (log.isErrorEnabled()) {
                    log.info("Resolved path file: " + path);
                }

                try {
                    boolean isComplete = BatchStatus.COMPLETED.equals(stepExecution.getStatus()) &&
                            stepExecution.getFailureExceptions().size() <= 0;
                    executionWithErrors = executionWithErrors || !isComplete;
                    if (!isComplete) {
                        String[] filename = file.replaceAll("\\\\", "/").split("/");
                        errorFilenames.add(filename[filename.length - 1].split("\\.",2)[0]);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug(hpanDirectory);
                        log.debug(file);
                    }
                    boolean isHpanFile = resolver.getPathMatcher().match(hpanDirectory, file);
                    if (deleteProcessedFiles || (isComplete && isHpanFile && manageHpanOnSuccess.equals("DELETE"))) {
                        if (log.isInfoEnabled()) {
                            log.info("Removing processed file: " + file);
                        }
                        FileUtils.forceDelete(FileUtils.getFile(path));
                    } else if (!isHpanFile || !isComplete || manageHpanOnSuccess.equals("ARCHIVE")) {
                        if (log.isInfoEnabled()) {
                            log.info("Archiving processed file: " + file);
                        }
                        archiveFile(file, path, isComplete);
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error(e.getMessage(), e);
                    }
                }

            }
        }

        if (deleteOutputFiles.equals("ALWAYS") || (deleteOutputFiles.equals("ERROR") && executionWithErrors)) {
            List<Resource> outputDirectoryResources =
                    Arrays.asList(resolver.getResources(outputDirectory + "/*"));
            outputDirectoryResources.forEach(outputDirectoryResource ->
            {
                if (deleteOutputFiles.equals("ALWAYS") || (errorFilenames.stream().anyMatch(
                        errorFilename -> {
                            try {
                                return outputDirectoryResource.getFile().getAbsolutePath().contains(errorFilename);
                            } catch (IOException e) {
                                if (log.isErrorEnabled()) {
                                    log.error(e.getMessage(),e);
                                }
                                return false;
                            }
                        }))
                ) {
                    try {
                        if (log.isInfoEnabled()) {
                            log.info("Deleting output file: " + outputDirectoryResource.getFile());
                        }
                        FileUtils.forceDelete(outputDirectoryResource.getFile());
                    } catch (IOException e) {
                        if (log.isErrorEnabled()) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            });
        }

        return RepeatStatus.FINISHED;
    }

    @SneakyThrows
    private void archiveFile(String file, String path, Boolean isCompleted) {
        String archivalPath = isCompleted ? successPath : errorPath;
        file = file.replaceAll("\\\\", "/");
        String[] filename = file.split("/");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        archivalPath = resolver.getResources(archivalPath)[0].getFile().getAbsolutePath();
        File destFile = FileUtils.getFile(archivalPath + "/" + RandomUtils.nextLong() +
                "_" + OffsetDateTime.now().format(fmt) + "_" + filename[filename.length - 1]);
        FileUtils.moveFile(FileUtils.getFile(path), destFile);
    }

}
