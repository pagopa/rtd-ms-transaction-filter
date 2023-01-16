package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.assertj.core.api.Assertions.assertThat;

import it.gov.pagopa.rtd.transaction_filter.batch.model.DeleteOutputFilesEnum;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;


class FileManagementTaskletTest {

    File successFile;
    File errorFile;
    File hpanFile;
    File errorHpanFile;
    File outputFileCsv;

    @TempDir
    Path tempDir;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private final String OUTPUT_PATH = "test/output";
    private final String PENDING_PATH = "test/output/pending";
    private final String SUCCESS_PATH = "test/success";
    private final String ERROR_PATH = "test/error";
    private final String HPAN_PATH = "test/hpan";
    private final String TRANSACTIONS_PATH = "test/trxs";
    private final String LOGS_PATH = "test/logs";

    @SneakyThrows
    @Test
    void testFileManagement_NoDeleteLocalFiles() {

        createDefaultDirectories();

        successFile = Files.createFile(tempDir.resolve(TRANSACTIONS_PATH + "/success-trx.csv")).toFile();
        errorFile = Files.createFile(tempDir.resolve(TRANSACTIONS_PATH + "/error-trx.csv")).toFile();
        hpanFile = Files.createFile(tempDir.resolve(HPAN_PATH + "/hpan.pgp")).toFile();
        errorHpanFile = Files.createFile(tempDir.resolve(HPAN_PATH + "/error-hpan.pgp")).toFile();
        Files.createFile(tempDir.resolve(OUTPUT_PATH + "/error-trx-output-file.pgp"));
        Files.createFile(tempDir.resolve(OUTPUT_PATH + "/success-trx-output-file.pgp"));
        Files.createFile(tempDir.resolve(OUTPUT_PATH + "/error-trx-output-file.csv"));
        outputFileCsv = Files.createFile(tempDir.resolve(OUTPUT_PATH + "/success-trx-output-file.csv")).toFile();

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setDeleteProcessedFiles(false);
        archivalTasklet.setDeleteOutputFiles(DeleteOutputFilesEnum.KEEP.name());
        archivalTasklet.setManageHpanOnSuccess("DELETE");

        assertThat(getSuccessFiles()).isEmpty();
        assertThat(getErrorFiles()).isEmpty();

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = createStepExecution("INPUT_OK",BatchStatus.COMPLETED, "file:" + successFile.getAbsolutePath());
        stepExecutions.add(stepExecution1);

        StepExecution stepExecution2 = createStepExecution("INPUT_FAILED", BatchStatus.FAILED, "file:" + errorFile.getAbsolutePath());
        stepExecutions.add(stepExecution2);

        StepExecution stepExecution3 = createStepExecution("HPAN_OK", BatchStatus.COMPLETED, "file:" + hpanFile.getAbsolutePath());
        stepExecutions.add(stepExecution3);

        StepExecution stepExecution4 = createStepExecution("HPAN_FAILED", BatchStatus.FAILED, "file:" + errorHpanFile.getAbsolutePath());
        stepExecutions.add(stepExecution4);

        StepExecution stepExecution6 = createStepExecution("OUTPUT_CSV_OK", BatchStatus.COMPLETED, "file:" + outputFileCsv.getAbsolutePath());
        stepExecutions.add(stepExecution6);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        assertThat(getSuccessFiles()).hasSize(1);
        assertThat(getErrorFiles()).hasSize(2);

        successFile.createNewFile();

        stepExecutions = new ArrayList<>();

        StepExecution stepExecution5 = createStepExecution("INPUT_OK", BatchStatus.COMPLETED, "file:" + successFile.getAbsolutePath());
        stepExecutions.add(stepExecution5);

        execution = MetaDataInstanceFactory.createStepExecution();
        stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        assertThat(getSuccessFiles()).hasSize(2);
        assertThat(getCsvOutputFiles()).hasSize(2);
        assertThat(getPgpOutputFiles()).hasSize(2);
    }

    @SneakyThrows
    @Test
    void testFileManagement_NoDeleteLocalFiles_WithSkips() {

        createDefaultDirectories();

        successFile = Files.createFile(tempDir.resolve(TRANSACTIONS_PATH + "/success-trx.csv")).toFile();
        errorFile = Files.createFile(tempDir.resolve(TRANSACTIONS_PATH + "/error-trx.csv")).toFile();
        hpanFile = Files.createFile(tempDir.resolve(HPAN_PATH + "/hpan.pgp")).toFile();
        errorHpanFile = Files.createFile(tempDir.resolve(HPAN_PATH + "/error-hpan.pgp")).toFile();
        Files.createFile(tempDir.resolve(OUTPUT_PATH + "/error-trx-output-file.pgp"));
        Files.createFile(tempDir.resolve(OUTPUT_PATH + "/success-trx-output-file.pgp"));
        Files.createFile(tempDir.resolve(OUTPUT_PATH + "/error-trx-output-file.csv"));

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setDeleteProcessedFiles(false);
        archivalTasklet.setDeleteOutputFiles(DeleteOutputFilesEnum.KEEP.name());
        archivalTasklet.setManageHpanOnSuccess("DELETE");

        assertThat(getSuccessFiles()).isEmpty();
        assertThat(getErrorFiles()).isEmpty();

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = createStepExecution("INPUT_OK",BatchStatus.COMPLETED, "file:" + successFile.getAbsolutePath());
        stepExecution1.setExitStatus(new ExitStatus("COMPLETED WITH SKIPS"));
        stepExecutions.add(stepExecution1);

        StepExecution stepExecution2 = createStepExecution("INPUT_FAILED", BatchStatus.FAILED, "file:" + errorFile.getAbsolutePath());
        stepExecutions.add(stepExecution2);

        StepExecution stepExecution3 = createStepExecution("HPAN_OK", BatchStatus.COMPLETED, "file:" + hpanFile.getAbsolutePath());
        stepExecutions.add(stepExecution3);

        StepExecution stepExecution4 = createStepExecution("HPAN_FAILED", BatchStatus.FAILED, "file:" + errorHpanFile.getAbsolutePath());
        stepExecutions.add(stepExecution4);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        assertThat(getSuccessFiles()).hasSize(1);
        assertThat(getErrorFiles()).hasSize(2);

        successFile.createNewFile();

        stepExecutions = new ArrayList<>();

        StepExecution stepExecution5 = createStepExecution("INPUT_OK", BatchStatus.COMPLETED, "file:" + successFile.getAbsolutePath());
        stepExecutions.add(stepExecution5);

        execution = MetaDataInstanceFactory.createStepExecution();
        stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        assertThat(getSuccessFiles()).hasSize(2);
        assertThat(getCsvOutputFiles()).hasSize(1);
        assertThat(getPgpOutputFiles()).hasSize(2);
    }

    @SneakyThrows
    @Test
    void testFileManagement_DeleteLocalFiles() {

        createDefaultDirectories();

        successFile = Files.createFile(tempDir.resolve(TRANSACTIONS_PATH + "/success-trx.csv")).toFile();
        errorFile = Files.createFile(tempDir.resolve(TRANSACTIONS_PATH + "/error-trx.csv")).toFile();
        hpanFile = Files.createFile(tempDir.resolve(HPAN_PATH + "/hpan.pgp")).toFile();
        errorHpanFile = Files.createFile(tempDir.resolve(HPAN_PATH + "/error-hpan.pgp")).toFile();
        File pgpFileFailed = Files.createFile(tempDir.resolve(OUTPUT_PATH + "/error-trx-output-file.pgp")).toFile();
        Files.createFile(tempDir.resolve(OUTPUT_PATH + "/success-trx-output-file.pgp"));
        File csvOutputFile = Files.createFile(tempDir.resolve(OUTPUT_PATH + "/error-trx-output-file.csv")).toFile();

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setDeleteProcessedFiles(true);
        archivalTasklet.setDeleteOutputFiles(DeleteOutputFilesEnum.ALWAYS.name());
        archivalTasklet.setManageHpanOnSuccess("DELETE");

        assertThat(getSuccessFiles()).isEmpty();
        assertThat(getErrorFiles()).isEmpty();

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = createStepExecution("INPUT_OK",BatchStatus.COMPLETED, "file:" + successFile.getAbsolutePath());
        stepExecutions.add(stepExecution1);

        StepExecution stepExecution2 = createStepExecution("INPUT_FAILED", BatchStatus.FAILED, "file:" + errorFile.getAbsolutePath());
        stepExecutions.add(stepExecution2);

        StepExecution stepExecution3 = createStepExecution("HPAN_OK", BatchStatus.COMPLETED, "file:" + hpanFile.getAbsolutePath());
        stepExecutions.add(stepExecution3);

        StepExecution stepExecution4 = createStepExecution("HPAN_FAILED", BatchStatus.FAILED, "file:" + errorHpanFile.getAbsolutePath());
        stepExecutions.add(stepExecution4);

        StepExecution stepExecution6 = createStepExecution("PGP_SEND_FAILED", BatchStatus.FAILED, "file:" + pgpFileFailed.getAbsolutePath());
        stepExecutions.add(stepExecution6);

        StepExecution stepExecution7 = createStepExecution("ENCRYPT_FILE_CSV", BatchStatus.FAILED, "file:" + csvOutputFile.getAbsolutePath());
        stepExecutions.add(stepExecution7);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        assertThat(getSuccessFiles()).isEmpty();
        assertThat(getErrorFiles()).isEmpty();

        successFile.createNewFile();

        stepExecutions = new ArrayList<>();

        StepExecution stepExecution5 = createStepExecution("INPUT_OK", BatchStatus.COMPLETED, "file:" + successFile.getAbsolutePath());
        stepExecutions.add(stepExecution5);

        execution = MetaDataInstanceFactory.createStepExecution();
        stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        assertThat(getSuccessFiles()).isEmpty();
        assertThat(getPgpOutputFiles()).isEmpty();
    }

    @SneakyThrows
    @Test
    void testFileManagement_DeleteOutputFilesOnErrors() {

        createDefaultDirectories();

        successFile = Files.createFile(tempDir.resolve(TRANSACTIONS_PATH + "/success-trx.csv")).toFile();
        errorFile = Files.createFile(tempDir.resolve(TRANSACTIONS_PATH + "/CSTAR.12345.TRNLOG.20221010.123456.001.csv")).toFile();
        File pgpFileFailed = Files.createFile(tempDir.resolve(OUTPUT_PATH + "/ADE.12345.20221010.123456.001.01.csv.pgp")).toFile();
        File csvOutputFile = Files.createFile(tempDir.resolve(OUTPUT_PATH + "/ADE.12345.20221010.123456.001.01.csv")).toFile();
        Files.createFile(tempDir.resolve(OUTPUT_PATH + "/success-trx-output-file.pgp"));
        // test file csv deletion without filename been explicitly handled by any step, only valid for RTD files right now
        Files.createFile(tempDir.resolve(OUTPUT_PATH + "/CSTAR.12345.TRNLOG.20221010.123456.001.csv"));

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setDeleteProcessedFiles(false);
        archivalTasklet.setDeleteOutputFiles(DeleteOutputFilesEnum.ERROR.name());
        archivalTasklet.setManageHpanOnSuccess("DELETE");

        assertThat(getSuccessFiles()).isEmpty();
        assertThat(getErrorFiles()).isEmpty();

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = createStepExecution("INPUT_OK",BatchStatus.COMPLETED, "file:" + successFile.getAbsolutePath());
        stepExecutions.add(stepExecution1);

        StepExecution stepExecution2 = createStepExecution("INPUT_FAILED", BatchStatus.FAILED, "file:" + errorFile.getAbsolutePath());
        stepExecutions.add(stepExecution2);

        StepExecution stepExecution3 = createStepExecution("PGP_SEND_FAILED", BatchStatus.FAILED, "file:" + pgpFileFailed.getAbsolutePath());
        stepExecutions.add(stepExecution3);

        StepExecution stepExecution4 = createStepExecution("ENCRYPT_FILE_CSV", BatchStatus.FAILED, "file:" + csvOutputFile.getAbsolutePath());
        stepExecutions.add(stepExecution4);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        assertThat(getSuccessFiles()).hasSize(1);
        assertThat(getErrorFiles()).hasSize(1);
        assertThat(getPgpPendingFiles()).hasSize(1);
        assertThat(getCsvOutputFiles()).isEmpty();
        assertThat(getPgpOutputFiles()).hasSize(1);

        successFile.createNewFile();

        stepExecutions = new ArrayList<>();

        StepExecution stepExecution5 = createStepExecution("INPUT_OK", BatchStatus.COMPLETED, "file:" + successFile.getAbsolutePath());
        stepExecutions.add(stepExecution5);

        execution = MetaDataInstanceFactory.createStepExecution();
        stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        assertThat(getSuccessFiles()).hasSize(2);
        assertThat(getPgpOutputFiles()).hasSize(1);
    }

    @SneakyThrows
    @Test
    void testFileManagement_KeepHpanInLocation() {

        createDefaultDirectories();

        successFile = Files.createFile(tempDir.resolve(TRANSACTIONS_PATH + "/success-trx.csv")).toFile();
        errorFile = Files.createFile(tempDir.resolve(TRANSACTIONS_PATH + "/error-trx.csv")).toFile();
        hpanFile = Files.createFile(tempDir.resolve(HPAN_PATH + "/hpan.pgp")).toFile();
        errorHpanFile = Files.createFile(tempDir.resolve(HPAN_PATH + "/error-hpan.pgp")).toFile();
        File pgpFileFailed = Files.createFile(tempDir.resolve(OUTPUT_PATH + "/error-trx-output-file.pgp")).toFile();
        Files.createFile(tempDir.resolve(OUTPUT_PATH + "/success-trx-output-file.pgp"));
        Files.createFile(tempDir.resolve(OUTPUT_PATH + "/error-trx-output-file.csv"));

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setDeleteProcessedFiles(false);
        archivalTasklet.setDeleteOutputFiles(DeleteOutputFilesEnum.ERROR.name());
        archivalTasklet.setManageHpanOnSuccess("KEEP");

        assertThat(getSuccessFiles()).isEmpty();
        assertThat(getHpanFiles()).hasSize(2);
        assertThat(getErrorFiles()).isEmpty();

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = createStepExecution("INPUT_FILE_SUCCESS", BatchStatus.COMPLETED, "file:" + successFile.getAbsolutePath());
        stepExecutions.add(stepExecution1);

        StepExecution stepExecution2 = createStepExecution("INPUT_FILE_ERROR", BatchStatus.FAILED, "file:" + errorFile.getAbsolutePath());
        stepExecutions.add(stepExecution2);

        StepExecution stepExecution3 = createStepExecution("HPAN_OK", BatchStatus.COMPLETED, "file:" + hpanFile.getAbsolutePath());
        stepExecutions.add(stepExecution3);

        StepExecution stepExecution4 = createStepExecution("HPAN_FAILED", BatchStatus.FAILED, "file:" + errorHpanFile.getAbsolutePath());
        stepExecutions.add(stepExecution4);

        StepExecution stepExecution6 = createStepExecution("PGP_SEND_FAILED", BatchStatus.FAILED, "file:" + pgpFileFailed.getAbsolutePath());
        stepExecutions.add(stepExecution6);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        assertThat(getSuccessFiles()).hasSize(1);
        assertThat(getHpanFiles()).hasSize(1);
        assertThat(getErrorFiles()).hasSize(2);

        successFile.createNewFile();

        stepExecutions = new ArrayList<>();

        StepExecution stepExecution5 = createStepExecution("E", BatchStatus.COMPLETED, successFile.getAbsolutePath());
        stepExecutions.add(stepExecution5);

        execution = MetaDataInstanceFactory.createStepExecution();
        stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        assertThat(getHpanFiles()).hasSize(1);
        assertThat(getSuccessFiles()).hasSize(2);
        assertThat(getPgpOutputFiles()).hasSize(1);
    }

    @SneakyThrows
    @Test
    void testFileManagement_ArchiveHpan() {

        createDefaultDirectories();

        hpanFile = Files.createFile(tempDir.resolve(HPAN_PATH + "/hpan.pgp")).toFile();
        errorHpanFile = Files.createFile(tempDir.resolve(HPAN_PATH + "/error-hpan.pgp")).toFile();

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setDeleteProcessedFiles(false);
        archivalTasklet.setDeleteOutputFiles(DeleteOutputFilesEnum.ERROR.name());
        archivalTasklet.setManageHpanOnSuccess("ARCHIVE");
        archivalTasklet.afterPropertiesSet();

        assertThat(getHpanFiles()).hasSize(2);

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = createStepExecution("HPAN_OK", BatchStatus.COMPLETED, "file:" + hpanFile.getAbsolutePath());
        stepExecutions.add(stepExecution1);

        StepExecution stepExecution2 = createStepExecution("HPAN_FAILED", BatchStatus.FAILED, "file:" + errorHpanFile.getAbsolutePath());
        stepExecutions.add(stepExecution2);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        assertThat(getSuccessFiles()).hasSize(1);
        assertThat(getHpanFiles()).isEmpty();
        assertThat(getErrorFiles()).hasSize(1);
    }

    @SneakyThrows
    @EnumSource(DeleteOutputFilesEnum.class)
    @ParameterizedTest
    void givenDeleteOutputFilesPolicyWhenRunFileManagementThenMovePgpOutputFilesToPendingFolder(DeleteOutputFilesEnum deleteOutputFilesFlag) {

        createDefaultDirectories();

        File outputFilePgp = Files.createFile(tempDir.resolve(OUTPUT_PATH + File.separator + "trx-output-file.pgp")).toFile();
        File outputFileCsv = Files.createFile(tempDir.resolve(OUTPUT_PATH + File.separator + "trx-output-file.csv")).toFile();

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setDeleteProcessedFiles(false);
        archivalTasklet.setDeleteOutputFiles(deleteOutputFilesFlag.name());
        archivalTasklet.setManageHpanOnSuccess("DELETE");

        // pre-condition on initial setup
        assertThat(getPgpOutputFiles()).hasSize(1);
        assertThat(getCsvOutputFiles()).hasSize(1);

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = createStepExecution("SEND_PGP_STEP", BatchStatus.FAILED, "file:" + outputFilePgp.getAbsolutePath());
        stepExecutions.add(stepExecution1);

        StepExecution stepExecution2 = createStepExecution("ENCRYPT_AGGREGATE_STEP", BatchStatus.COMPLETED, "file:" + outputFileCsv.getAbsolutePath());
        stepExecutions.add(stepExecution2);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        // the file pgp has been moved from output to output/pending
        assertThat(getPgpPendingFiles()).hasSize(1);
        switch (deleteOutputFilesFlag) {
            case KEEP:
                assertThat(getCsvOutputFiles()).hasSize(1); break;
            case ERROR:
            case ALWAYS:
                assertThat(getCsvOutputFiles()).isEmpty();
        }
    }

    @SneakyThrows
    @Test
    void whenLogFilesAreEmptyThenDeleteThem() {
        createDefaultDirectories();

        hpanFile =  Files.createFile(tempDir.resolve(HPAN_PATH + File.separator + "hpan.pgp")).toFile();

        Files.createFile(tempDir.resolve(LOGS_PATH + File.separator + "empty-log-file.csv"));
        File logFileNotEmpty = Files.createFile(tempDir.resolve(LOGS_PATH + File.separator + "not-empty-log-file.csv")).toFile();
        FileUtils.write(logFileNotEmpty, "this;is;a;not;empty;log", Charset.defaultCharset());

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        Collection<File> logFiles = getLogFiles();

        assertThat(logFiles).isNotEmpty().hasSize(1).contains(logFileNotEmpty);
    }

    @SneakyThrows
    @Test
    void whenLogDirectoryIsNotSetThenTaskletDoNotDeleteEmptyLogs() {
        createDefaultDirectories();

        hpanFile =  Files.createFile(tempDir.resolve(HPAN_PATH + File.separator + "hpan.pgp")).toFile();

        Files.createFile(tempDir.resolve(LOGS_PATH + File.separator + "empty-log-file.csv"));
        File logFileNotEmpty = Files.createFile(tempDir.resolve(LOGS_PATH + File.separator + "not-empty-log-file.csv")).toFile();
        FileUtils.write(logFileNotEmpty, "this;is;a;not;empty;log", Charset.defaultCharset());

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setLogsDirectory(null);

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        Collection<File> logFiles = getLogFiles();

        assertThat(logFiles).isNotEmpty().hasSize(2);
    }

    @SneakyThrows
    @Test
    void whenThereAreMoreStepsWithSameFilenameThenEvaluateWorstStatus() {

        createDefaultDirectories();

        File inputFile = Files.createFile(tempDir.resolve(TRANSACTIONS_PATH + "/success-trx.csv")).toFile();

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setDeleteProcessedFiles(false);
        archivalTasklet.setDeleteOutputFiles(DeleteOutputFilesEnum.KEEP.name());
        archivalTasklet.setManageHpanOnSuccess("KEEP");

        assertThat(getSuccessFiles()).isEmpty();
        assertThat(getErrorFiles()).isEmpty();

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = createStepExecution("INPUT_FILE_SUCCESS_EG_CHECKSUM", BatchStatus.COMPLETED, "file:" + inputFile.getAbsolutePath());
        stepExecutions.add(stepExecution1);

        StepExecution stepExecution2 = createStepExecution("INPUT_FILE_ERROR_EG_TRANSACTION_PROCESS", BatchStatus.FAILED, "file:" + inputFile.getAbsolutePath());
        stepExecutions.add(stepExecution2);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        // the FAILED status is evaluated and the file is moved into "error" folder
        assertThat(getSuccessFiles()).isEmpty();
        assertThat(getErrorFiles()).hasSize(1);

        // invert the orders of the steps and retest
        stepExecutions = new ArrayList<>();
        stepExecution1 = createStepExecution("INPUT_FILE_ERROR_EG_TRANSACTION_PROCESS", BatchStatus.FAILED, "file:" + inputFile.getAbsolutePath());
        stepExecutions.add(stepExecution1);

        stepExecution2 = createStepExecution("INPUT_FILE_SUCCESS_EG_CHECKSUM", BatchStatus.COMPLETED, "file:" + inputFile.getAbsolutePath());
        stepExecutions.add(stepExecution2);

        stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        // the assertion must be step order-independent
        assertThat(getSuccessFiles()).isEmpty();
        assertThat(getErrorFiles()).hasSize(1);
    }

    @SneakyThrows
    @EnumSource(DeleteOutputFilesEnum.class)
    @ParameterizedTest
    void givenPendingFilesWhenSendPendingStepFailThenFilesAreNotMoved(DeleteOutputFilesEnum deleteOutputFilesFlag) {

        createDefaultDirectories();

        File pendingFile = Files.createFile(tempDir.resolve(PENDING_PATH + File.separator + "file-to-send-again.pgp")).toFile();

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setDeleteProcessedFiles(false);
        archivalTasklet.setDeleteOutputFiles(deleteOutputFilesFlag.name());
        archivalTasklet.setManageHpanOnSuccess("DELETE");

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = createStepExecution("SEND_PENDING_STEP", BatchStatus.FAILED, "file:" + pendingFile.getAbsolutePath());
        stepExecutions.add(stepExecution1);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        // the file pgp has been moved from output to output/pending
        assertThat(getPgpPendingFiles()).hasSize(1);
    }

    @SneakyThrows
    @EnumSource(DeleteOutputFilesEnum.class)
    @ParameterizedTest
    void givenPendingFilesWhenSendPendingStepSuccessThenFilesAreDeleted(DeleteOutputFilesEnum deleteOutputFilesFlag) {

        createDefaultDirectories();

        File pendingFile = Files.createFile(tempDir.resolve(PENDING_PATH + File.separator + "file-to-send-again.pgp")).toFile();

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setDeleteProcessedFiles(false);
        archivalTasklet.setDeleteOutputFiles(deleteOutputFilesFlag.name());
        archivalTasklet.setManageHpanOnSuccess("DELETE");

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = createStepExecution("SEND_PENDING_STEP", BatchStatus.COMPLETED, "file:" + pendingFile.getAbsolutePath());
        stepExecutions.add(stepExecution1);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        // the file pgp has been moved from output to output/pending
        assertThat(getPgpPendingFiles()).isEmpty();
    }

    @SneakyThrows
    @Test
    void givenOutputFilesFromPreviousRunsWhenDeleteOutputFilesIsAlwaysThenDoRemoveOldFilesToo() {
        createDefaultDirectories();

        Files.createFile(tempDir.resolve(OUTPUT_PATH + File.separator + "old-file.pgp"));
        File outputFileToSend = Files.createFile(tempDir.resolve(OUTPUT_PATH + File.separator + "file-to-send.pgp")).toFile();

        FileManagementTasklet archivalTasklet = createTaskletWithDefaultDirectories();
        archivalTasklet.setDeleteProcessedFiles(false);
        archivalTasklet.setDeleteOutputFiles(DeleteOutputFilesEnum.ALWAYS.name());
        archivalTasklet.setManageHpanOnSuccess("DELETE");

        assertThat(getPgpOutputFiles()).hasSize(2);

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        List<StepExecution> stepExecutions = new ArrayList<>();

        StepExecution stepExecution1 = createStepExecution("SEND_OUTPUT_FILE", BatchStatus.COMPLETED, "file:" + outputFileToSend.getAbsolutePath());
        stepExecutions.add(stepExecution1);

        StepContext stepContext = new StepContext(execution);
        stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        // the file pgp has been moved from output to output/pending
        assertThat(getPgpOutputFiles()).isEmpty();
    }

    private FileManagementTasklet createTaskletWithDefaultDirectories() {
        FileManagementTasklet archivalTasklet = new FileManagementTasklet();
        archivalTasklet.setUploadPendingPath("file:" + tempDir + File.separator + PENDING_PATH);
        archivalTasklet.setSuccessPath("file:" + tempDir + File.separator + SUCCESS_PATH);
        archivalTasklet.setOutputDirectory("file:" + tempDir + File.separator + OUTPUT_PATH);
        archivalTasklet.setHpanDirectory("file:" + tempDir + File.separator + HPAN_PATH + "/*.pgp");
        archivalTasklet.setLogsDirectory("file:" + tempDir + File.separator + LOGS_PATH);
        archivalTasklet.setErrorPath("file:" + tempDir + File.separator + ERROR_PATH);

        return archivalTasklet;
    }

    @SneakyThrows
    private void createDefaultDirectories() {
//        Files.createDirectory(tempDir.resolve("test"));
        Files.createDirectories(tempDir.resolve(PENDING_PATH));
        Files.createDirectory(tempDir.resolve(SUCCESS_PATH));
        Files.createDirectory(tempDir.resolve(ERROR_PATH));
        Files.createDirectory(tempDir.resolve(HPAN_PATH));
        Files.createDirectory(tempDir.resolve(TRANSACTIONS_PATH));
        Files.createDirectory(tempDir.resolve(LOGS_PATH));
    }

    private StepExecution createStepExecution(String stepName, BatchStatus status, String filename) {
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(stepName, 1L);
        stepExecution.setStatus(status);
        stepExecution.getExecutionContext().put("fileName", filename);
        return stepExecution;
    }

    @SneakyThrows
    private Collection<File> getHpanFiles() {
        return FileUtils.listFiles(
            resolver.getResources("file:" + tempDir + File.separator + HPAN_PATH)[0].getFile(),
            null,false);
    }

    @SneakyThrows
    private Collection<File> getSuccessFiles() {
        return FileUtils.listFiles(
            resolver.getResources("file:" + tempDir + File.separator + SUCCESS_PATH)[0].getFile(),
            null,false);
    }

    @SneakyThrows
    private Collection<File> getErrorFiles() {
        return FileUtils.listFiles(
            resolver.getResources("file:" + tempDir + File.separator + ERROR_PATH)[0].getFile(),
            null,false);
    }

    @SneakyThrows
    private Collection<File> getLogFiles() {
        return FileUtils.listFiles(
            resolver.getResources("file:" + tempDir + File.separator + LOGS_PATH)[0].getFile(),
            new String[]{"csv"},false);
    }

    @SneakyThrows
    private Collection<File> getPgpOutputFiles() {
        return FileUtils.listFiles(
            resolver.getResources("file:" + tempDir + File.separator + OUTPUT_PATH)[0].getFile(),
            new String[]{"pgp"},false);
    }

    @SneakyThrows
    private Collection<File> getCsvOutputFiles() {
        return FileUtils.listFiles(
            resolver.getResources("file:" + tempDir + File.separator + OUTPUT_PATH)[0].getFile(),
            new String[]{"csv"},false);
    }

    private Collection<File> getPgpPendingFiles() {
        return FileUtils.listFiles(
            new File(tempDir + File.separator + PENDING_PATH),
            new String[]{"pgp"},false);
    }

    @SneakyThrows
    @AfterEach
    void tearDown() {
        FileUtils.forceDelete(tempDir.toFile());
    }
}