package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;


public class FileManagementTaskletTest {

    File successFile;
    File errorFile;
    File hpanFile;
    File errorHpanFile;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @Test
    public void testFileManagement_NoDeleteLocalFiles() {

        try {

            tempFolder.newFolder("test1");
            tempFolder.newFolder("test1","success");
            tempFolder.newFolder("test1","error");
            tempFolder.newFolder("test1","output");
            tempFolder.newFolder("test1","hpan");
            tempFolder.newFolder("test1","trxs");

            successFile = tempFolder.newFile("test1/trxs/success-trx.pgp");
            errorFile =  tempFolder.newFile("test1/trxs/error-trx.pgp");
            hpanFile =  tempFolder.newFile("test1/hpan/hpan.pgp");
            errorHpanFile = tempFolder.newFile("test1/hpan/error-hpan.pgp");
            tempFolder.newFile("test1/output/error-trx-output-file.pgp");
            tempFolder.newFile("test1/output/success-trx-output-file.pgp");
            tempFolder.newFile("test1/output/error-trx-output-file.csv");

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            FileManagementTasklet archivalTasklet = new FileManagementTasklet();
            archivalTasklet.setUploadPendingPath("classpath:/test-encrypt/**/test1/error");
            archivalTasklet.setSuccessPath("classpath:/test-encrypt/**/test1/success");
            archivalTasklet.setOutputDirectory("classpath:/test-encrypt/**/test1/output");
            archivalTasklet.setHpanDirectory("file:/"+resolver.getResources(
                    "classpath:/test-encrypt/**/test1/hpan")[0].getFile().getAbsolutePath()+"/*.pgp");
            archivalTasklet.setDeleteProcessedFiles(false);
            archivalTasklet.setDeleteOutputFiles("NEVER");
            archivalTasklet.setManageHpanOnSuccess("DELETE");

            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/success")[0].getFile(),
                            new String[]{"pgp"},false).size());
            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/error")[0].getFile(),
                            new String[]{"pgp"},false).size());
            StepExecution execution = MetaDataInstanceFactory.createStepExecution();

            List<StepExecution> stepExecutions = new ArrayList<>();

            StepExecution stepExecution1 = MetaDataInstanceFactory.createStepExecution("A",1L);
            stepExecution1.setStatus(BatchStatus.COMPLETED);
            stepExecution1.getExecutionContext().put("fileName", successFile.getAbsolutePath());
            stepExecutions.add(stepExecution1);

            StepExecution stepExecution2 = MetaDataInstanceFactory.createStepExecution("B", 1L);
            stepExecution2.setStatus(BatchStatus.FAILED);
            stepExecution2.getExecutionContext().put("fileName", errorFile.getAbsolutePath());
            stepExecutions.add(stepExecution2);

            StepExecution stepExecution3 = MetaDataInstanceFactory.createStepExecution("C", 1L);
            stepExecution3.setStatus(BatchStatus.COMPLETED);
            stepExecution3.getExecutionContext().put("fileName", hpanFile.getAbsolutePath());
            stepExecutions.add(stepExecution3);

            StepExecution stepExecution4 = MetaDataInstanceFactory.createStepExecution("D", 1L);
            stepExecution4.setStatus(BatchStatus.FAILED);
            stepExecution4.getExecutionContext().put("fileName", errorHpanFile.getAbsolutePath());
            stepExecutions.add(stepExecution4);

            StepContext stepContext = new StepContext(execution);
            stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
            ChunkContext chunkContext = new ChunkContext(stepContext);

            archivalTasklet.execute(new StepContribution(execution),chunkContext);

            Assert.assertEquals(1,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/success")[0].getFile(),
                            new String[]{"pgp"},false).size());
            Assert.assertEquals(2,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/error")[0].getFile(),
                            new String[]{"pgp"},false).size());

            successFile.createNewFile();

            stepExecutions = new ArrayList<>();

            StepExecution stepExecution5 = MetaDataInstanceFactory.createStepExecution("E", 1L);
            stepExecution5.setStatus(BatchStatus.COMPLETED);
            stepExecution5.getExecutionContext().put("fileName",successFile.getAbsolutePath());
            stepExecutions.add(stepExecution5);

            execution = MetaDataInstanceFactory.createStepExecution();
            stepContext = new StepContext(execution);
            stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
            chunkContext = new ChunkContext(stepContext);

            archivalTasklet.execute(new StepContribution(execution),chunkContext);

            Assert.assertEquals(2,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/success")[0].getFile(),
                            new String[]{"pgp"},false).size());

            Assert.assertEquals(2,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/output")[0].getFile(),
                            new String[]{"pgp"},false).size());

            Assert.assertEquals(1,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/output")[0].getFile(),
                            new String[]{"csv"},false).size());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testFileManagement_NoDeleteLocalFiles_WithSkips() {

        try {

            tempFolder.newFolder("test1");
            tempFolder.newFolder("test1","success");
            tempFolder.newFolder("test1","error");
            tempFolder.newFolder("test1","output");
            tempFolder.newFolder("test1","hpan");
            tempFolder.newFolder("test1","trxs");

            successFile = tempFolder.newFile("test1/trxs/success-trx.pgp");
            errorFile =  tempFolder.newFile("test1/trxs/error-trx.pgp");
            hpanFile =  tempFolder.newFile("test1/hpan/hpan.pgp");
            errorHpanFile = tempFolder.newFile("test1/hpan/error-hpan.pgp");
            tempFolder.newFile("test1/output/error-trx-output-file.pgp");
            tempFolder.newFile("test1/output/success-trx-output-file.pgp");
            tempFolder.newFile("test1/output/error-trx-output-file.csv");

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            FileManagementTasklet archivalTasklet = new FileManagementTasklet();
            archivalTasklet.setUploadPendingPath("classpath:/test-encrypt/**/test1/error");
            archivalTasklet.setSuccessPath("classpath:/test-encrypt/**/test1/success");
            archivalTasklet.setOutputDirectory("classpath:/test-encrypt/**/test1/output");
            archivalTasklet.setHpanDirectory("file:/"+resolver.getResources(
                    "classpath:/test-encrypt/**/test1/hpan")[0].getFile().getAbsolutePath()+"/*.pgp");
            archivalTasklet.setDeleteProcessedFiles(false);
            archivalTasklet.setDeleteOutputFiles("NEVER");
            archivalTasklet.setManageHpanOnSuccess("DELETE");

            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/success")[0].getFile(),
                            new String[]{"pgp"},false).size());
            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/error")[0].getFile(),
                            new String[]{"pgp"},false).size());
            StepExecution execution = MetaDataInstanceFactory.createStepExecution();

            List<StepExecution> stepExecutions = new ArrayList<>();

            StepExecution stepExecution1 = MetaDataInstanceFactory.createStepExecution("A",1L);
            stepExecution1.setStatus(BatchStatus.COMPLETED);
            stepExecution1.setExitStatus(new ExitStatus("COMPLETED WITH SKIPS"));
            stepExecution1.getExecutionContext().put("fileName", successFile.getAbsolutePath());
            stepExecutions.add(stepExecution1);

            StepExecution stepExecution2 = MetaDataInstanceFactory.createStepExecution("B", 1L);
            stepExecution2.setStatus(BatchStatus.FAILED);
            stepExecution2.getExecutionContext().put("fileName", errorFile.getAbsolutePath());
            stepExecutions.add(stepExecution2);

            StepExecution stepExecution3 = MetaDataInstanceFactory.createStepExecution("C", 1L);
            stepExecution3.setStatus(BatchStatus.COMPLETED);
            stepExecution3.getExecutionContext().put("fileName", hpanFile.getAbsolutePath());
            stepExecutions.add(stepExecution3);

            StepExecution stepExecution4 = MetaDataInstanceFactory.createStepExecution("D", 1L);
            stepExecution4.setStatus(BatchStatus.FAILED);
            stepExecution4.getExecutionContext().put("fileName", errorHpanFile.getAbsolutePath());
            stepExecutions.add(stepExecution4);

            StepContext stepContext = new StepContext(execution);
            stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
            ChunkContext chunkContext = new ChunkContext(stepContext);

            archivalTasklet.execute(new StepContribution(execution),chunkContext);

            Assert.assertEquals(1,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/success")[0].getFile(),
                            new String[]{"pgp"},false).size());
            Assert.assertEquals(2,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/error")[0].getFile(),
                            new String[]{"pgp"},false).size());

            successFile.createNewFile();

            stepExecutions = new ArrayList<>();

            StepExecution stepExecution5 = MetaDataInstanceFactory.createStepExecution("E", 1L);
            stepExecution5.setStatus(BatchStatus.COMPLETED);
            stepExecution5.getExecutionContext().put("fileName",successFile.getAbsolutePath());
            stepExecutions.add(stepExecution5);

            execution = MetaDataInstanceFactory.createStepExecution();
            stepContext = new StepContext(execution);
            stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
            chunkContext = new ChunkContext(stepContext);

            archivalTasklet.execute(new StepContribution(execution),chunkContext);

            Assert.assertEquals(2,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/success")[0].getFile(),
                            new String[]{"pgp"},false).size());

            Assert.assertEquals(2,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/output")[0].getFile(),
                            new String[]{"pgp"},false).size());

            Assert.assertEquals(1,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test1/output")[0].getFile(),
                            new String[]{"csv"},false).size());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testFileManagement_DeleteLocalFiles() {

        try {

            tempFolder.newFolder("test2");
            tempFolder.newFolder("test2","success");
            tempFolder.newFolder("test2","error");
            tempFolder.newFolder("test2","output");
            tempFolder.newFolder("test2","hpan");
            tempFolder.newFolder("test2","trxs");

            successFile = tempFolder.newFile("test2/trxs/success-trx.pgp");
            errorFile =  tempFolder.newFile("test2/trxs/error-trx.pgp");
            hpanFile =  tempFolder.newFile("test2/hpan/hpan.pgp");
            errorHpanFile = tempFolder.newFile("test2/hpan/error-hpan.pgp");
            tempFolder.newFile("test2/output/error-trx-output-file.pgp");
            tempFolder.newFile("test2/output/success-trx-output-file.pgp");
            tempFolder.newFile("test2/output/error-trx-output-file.csv");

            FileManagementTasklet archivalTasklet = new FileManagementTasklet();
            archivalTasklet.setUploadPendingPath("classpath:/test-encrypt/**/test2/error");
            archivalTasklet.setSuccessPath("classpath:/test-encrypt/**/test2/success");
            archivalTasklet.setOutputDirectory("classpath:/test-encrypt/**/test2/output");
            archivalTasklet.setHpanDirectory("classpath:/test-encrypt/**/test2/hpan");
            archivalTasklet.setDeleteProcessedFiles(true);
            archivalTasklet.setDeleteOutputFiles("ALWAYS");
            archivalTasklet.setManageHpanOnSuccess("DELETE");

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test2/success")[0].getFile(),
                            new String[]{"pgp"},false).size());
            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test2/error")[0].getFile(),
                            new String[]{"pgp"},false).size());
            StepExecution execution = MetaDataInstanceFactory.createStepExecution();

            List<StepExecution> stepExecutions = new ArrayList<>();

            StepExecution stepExecution1 = MetaDataInstanceFactory.createStepExecution("A",1L);
            stepExecution1.setStatus(BatchStatus.COMPLETED);
            stepExecution1.getExecutionContext().put("fileName", successFile.getAbsolutePath());
            stepExecutions.add(stepExecution1);

            StepExecution stepExecution2 = MetaDataInstanceFactory.createStepExecution("B", 1L);
            stepExecution2.setStatus(BatchStatus.FAILED);
            stepExecution2.getExecutionContext().put("fileName", errorFile.getAbsolutePath());
            stepExecutions.add(stepExecution2);

            StepExecution stepExecution3 = MetaDataInstanceFactory.createStepExecution("C", 1L);
            stepExecution3.setStatus(BatchStatus.COMPLETED);
            stepExecution3.getExecutionContext().put("fileName", hpanFile.getAbsolutePath());
            stepExecutions.add(stepExecution3);

            StepExecution stepExecution4 = MetaDataInstanceFactory.createStepExecution("D", 1L);
            stepExecution4.setStatus(BatchStatus.FAILED);
            stepExecution4.getExecutionContext().put("fileName", errorHpanFile.getAbsolutePath());
            stepExecutions.add(stepExecution4);

            StepContext stepContext = new StepContext(execution);
            stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
            ChunkContext chunkContext = new ChunkContext(stepContext);

            archivalTasklet.execute(new StepContribution(execution),chunkContext);

            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test2/success")[0].getFile(),
                            new String[]{"pgp"},false).size());
            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test2/error")[0].getFile(),
                            new String[]{"pgp"},false).size());

            successFile.createNewFile();

            stepExecutions = new ArrayList<>();

            StepExecution stepExecution5 = MetaDataInstanceFactory.createStepExecution("E", 1L);
            stepExecution5.setStatus(BatchStatus.COMPLETED);
            stepExecution5.getExecutionContext().put("fileName",successFile.getAbsolutePath());
            stepExecutions.add(stepExecution5);

            execution = MetaDataInstanceFactory.createStepExecution();
            stepContext = new StepContext(execution);
            stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
            chunkContext = new ChunkContext(stepContext);

            archivalTasklet.execute(new StepContribution(execution),chunkContext);

            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test2/success")[0].getFile(),
                            new String[]{"pgp"},false).size());

            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test2/output")[0].getFile(),
                            new String[]{"pgp"},false).size());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testFileManagement_DeleteOutputFilesOnErrors() {

        try {

            tempFolder.newFolder("test3");
            tempFolder.newFolder("test3","success");
            tempFolder.newFolder("test3","error");
            tempFolder.newFolder("test3","output");
            tempFolder.newFolder("test3","hpan");
            tempFolder.newFolder("test3","trxs");

            successFile = tempFolder.newFile("test3/trxs/success-trx.pgp");
            errorFile =  tempFolder.newFile("test3/trxs/error-trx.pgp");
            hpanFile =  tempFolder.newFile("test3/hpan/hpan.pgp");
            errorHpanFile = tempFolder.newFile("test3/hpan/error-hpan.pgp");
            tempFolder.newFile("test3/output/error-trx-output-file.pgp");
            tempFolder.newFile("test3/output/success-trx-output-file.pgp");
            tempFolder.newFile("test3/output/error-trx-output-file.csv");

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            FileManagementTasklet archivalTasklet = new FileManagementTasklet();
            archivalTasklet.setUploadPendingPath("classpath:/test-encrypt/**/test3/error");
            archivalTasklet.setSuccessPath("classpath:/test-encrypt/**/test3/success");
            archivalTasklet.setOutputDirectory("classpath:/test-encrypt/**/test3/output");
            archivalTasklet.setHpanDirectory("file:/"+resolver.getResources(
                    "classpath:/test-encrypt/**/test3/hpan")[0].getFile().getAbsolutePath()+"/*.pgp");
            archivalTasklet.setDeleteProcessedFiles(false);
            archivalTasklet.setDeleteOutputFiles("ERROR");
            archivalTasklet.setManageHpanOnSuccess("DELETE");

            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test3/success")[0].getFile(),
                            new String[]{"pgp"},false).size());
            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test3/error")[0].getFile(),
                            new String[]{"pgp"},false).size());
            StepExecution execution = MetaDataInstanceFactory.createStepExecution();

            List<StepExecution> stepExecutions = new ArrayList<>();

            StepExecution stepExecution1 = MetaDataInstanceFactory.createStepExecution("A",1L);
            stepExecution1.setStatus(BatchStatus.COMPLETED);
            stepExecution1.getExecutionContext().put("fileName", successFile.getAbsolutePath());
            stepExecutions.add(stepExecution1);

            StepExecution stepExecution2 = MetaDataInstanceFactory.createStepExecution("B", 1L);
            stepExecution2.setStatus(BatchStatus.FAILED);
            stepExecution2.getExecutionContext().put("fileName", errorFile.getAbsolutePath());
            stepExecutions.add(stepExecution2);

            StepExecution stepExecution3 = MetaDataInstanceFactory.createStepExecution("C", 1L);
            stepExecution3.setStatus(BatchStatus.COMPLETED);
            stepExecution3.getExecutionContext().put("fileName", hpanFile.getAbsolutePath());
            stepExecutions.add(stepExecution3);

            StepExecution stepExecution4 = MetaDataInstanceFactory.createStepExecution("D", 1L);
            stepExecution4.setStatus(BatchStatus.FAILED);
            stepExecution4.getExecutionContext().put("fileName", errorHpanFile.getAbsolutePath());
            stepExecutions.add(stepExecution4);

            StepContext stepContext = new StepContext(execution);
            stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
            ChunkContext chunkContext = new ChunkContext(stepContext);

            archivalTasklet.execute(new StepContribution(execution),chunkContext);

            Assert.assertEquals(1,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test3/success")[0].getFile(),
                            new String[]{"pgp"},false).size());
            Assert.assertEquals(2,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test3/error")[0].getFile(),
                            new String[]{"pgp"},false).size());

            successFile.createNewFile();

            stepExecutions = new ArrayList<>();

            StepExecution stepExecution5 = MetaDataInstanceFactory.createStepExecution("E", 1L);
            stepExecution5.setStatus(BatchStatus.COMPLETED);
            stepExecution5.getExecutionContext().put("fileName",successFile.getAbsolutePath());
            stepExecutions.add(stepExecution5);

            execution = MetaDataInstanceFactory.createStepExecution();
            stepContext = new StepContext(execution);
            stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
            chunkContext = new ChunkContext(stepContext);

            archivalTasklet.execute(new StepContribution(execution),chunkContext);

            Assert.assertEquals(2,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test3/success")[0].getFile(),
                            new String[]{"pgp"},false).size());

            Assert.assertEquals(1,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test3/output")[0].getFile(),
                            new String[]{"pgp"},false).size());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testFileManagement_KeepHpanInLocation() {

        try {

            tempFolder.newFolder("test4");
            tempFolder.newFolder("test4","success");
            tempFolder.newFolder("test4","error");
            tempFolder.newFolder("test4","output");
            tempFolder.newFolder("test4","hpan");
            tempFolder.newFolder("test4","trxs");

            successFile = tempFolder.newFile("test4/trxs/success-trx.pgp");
            errorFile =  tempFolder.newFile("test4/trxs/error-trx.pgp");
            hpanFile =  tempFolder.newFile("test4/hpan/hpan.pgp");
            errorHpanFile = tempFolder.newFile("test4/hpan/error-hpan.pgp");
            tempFolder.newFile("test4/output/error-trx-output-file.pgp");
            tempFolder.newFile("test4/output/success-trx-output-file.pgp");
            tempFolder.newFile("test4/output/error-trx-output-file.csv");

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            FileManagementTasklet archivalTasklet = new FileManagementTasklet();
            archivalTasklet.setUploadPendingPath("classpath:/test-encrypt/**/test4/error");
            archivalTasklet.setSuccessPath("classpath:/test-encrypt/**/test4/success");
            archivalTasklet.setOutputDirectory("classpath:/test-encrypt/**/test4/output");
            archivalTasklet.setHpanDirectory("file:/"+resolver.getResources(
                    "classpath:/test-encrypt/**/hpan")[0].getFile().getAbsolutePath()+"/*.pgp");
            archivalTasklet.setDeleteProcessedFiles(false);
            archivalTasklet.setDeleteOutputFiles("ERROR");
            archivalTasklet.setManageHpanOnSuccess("KEEP");

            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test4/success")[0].getFile(),
                            new String[]{"pgp"},false).size());
            Assert.assertEquals(0,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test4/error")[0].getFile(),
                            new String[]{"pgp"},false).size());
            StepExecution execution = MetaDataInstanceFactory.createStepExecution();

            List<StepExecution> stepExecutions = new ArrayList<>();

            StepExecution stepExecution1 = MetaDataInstanceFactory.createStepExecution("A",1L);
            stepExecution1.setStatus(BatchStatus.COMPLETED);
            stepExecution1.getExecutionContext().put("fileName", successFile.getAbsolutePath());
            stepExecutions.add(stepExecution1);

            StepExecution stepExecution2 = MetaDataInstanceFactory.createStepExecution("B", 1L);
            stepExecution2.setStatus(BatchStatus.FAILED);
            stepExecution2.getExecutionContext().put("fileName", errorFile.getAbsolutePath());
            stepExecutions.add(stepExecution2);

            StepExecution stepExecution3 = MetaDataInstanceFactory.createStepExecution("C", 1L);
            stepExecution3.setStatus(BatchStatus.COMPLETED);
            stepExecution3.getExecutionContext().put("fileName", hpanFile.getAbsolutePath());
            stepExecutions.add(stepExecution3);

            StepExecution stepExecution4 = MetaDataInstanceFactory.createStepExecution("D", 1L);
            stepExecution4.setStatus(BatchStatus.FAILED);
            stepExecution4.getExecutionContext().put("fileName", errorHpanFile.getAbsolutePath());
            stepExecutions.add(stepExecution4);

            StepContext stepContext = new StepContext(execution);
            stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
            ChunkContext chunkContext = new ChunkContext(stepContext);

            archivalTasklet.execute(new StepContribution(execution),chunkContext);

            Assert.assertEquals(1,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test4/success")[0].getFile(),
                            new String[]{"pgp"},false).size());
            Assert.assertEquals(2,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test4/error")[0].getFile(),
                            new String[]{"pgp"},false).size());

            successFile.createNewFile();

            stepExecutions = new ArrayList<>();

            StepExecution stepExecution5 = MetaDataInstanceFactory.createStepExecution("E", 1L);
            stepExecution5.setStatus(BatchStatus.COMPLETED);
            stepExecution5.getExecutionContext().put("fileName",successFile.getAbsolutePath());
            stepExecutions.add(stepExecution5);

            execution = MetaDataInstanceFactory.createStepExecution();
            stepContext = new StepContext(execution);
            stepContext.getStepExecution().getJobExecution().addStepExecutions(stepExecutions);
            chunkContext = new ChunkContext(stepContext);

            archivalTasklet.execute(new StepContribution(execution),chunkContext);

            Assert.assertEquals(1,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test4/hpan")[0].getFile(),
                            new String[]{"pgp"},false).size());

            Assert.assertEquals(2,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test4/success")[0].getFile(),
                            new String[]{"pgp"},false).size());

            Assert.assertEquals(1,
                    FileUtils.listFiles(
                            resolver.getResources("classpath:/test-encrypt/**/test4/output")[0].getFile(),
                            new String[]{"pgp"},false).size());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @SneakyThrows
    @Test
    public void whenLogFilesAreEmptyThenDeleteThem() {
        tempFolder.newFolder("test");
        tempFolder.newFolder("test","hpan");
        tempFolder.newFolder("test","logs");

        hpanFile =  tempFolder.newFile("test/hpan/hpan.pgp");

        tempFolder.newFile("test/logs/empty-log-file.csv");
        File logFileNotEmpty = tempFolder.newFile("test/logs/not-empty-log-file.csv");
        FileUtils.write(logFileNotEmpty, "this;is;a;not;empty;log");

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        FileManagementTasklet archivalTasklet = new FileManagementTasklet();
        archivalTasklet.setHpanDirectory("file:/"+resolver.getResources(
            "classpath:/test-encrypt/**/hpan")[0].getFile().getAbsolutePath()+"/*.pgp");
        archivalTasklet.setLogsDirectory("classpath:/test-encrypt/**/test/logs");

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        Collection<File> logFiles = FileUtils.listFiles(
            resolver.getResources("classpath:/test-encrypt/**/test/logs")[0].getFile(),
            new String[]{"csv"},false);

        assertThat(logFiles).isNotEmpty().hasSize(1).contains(logFileNotEmpty);
    }

    @SneakyThrows
    @Test
    public void whenLogDirectoryIsNotSetThenTaskletDoNotDeleteEmptyLogs() {
        tempFolder.newFolder("test");
        tempFolder.newFolder("test","hpan");
        tempFolder.newFolder("test","logs");

        hpanFile =  tempFolder.newFile("test/hpan/hpan.pgp");

        tempFolder.newFile("test/logs/empty-log-file.csv");
        File logFileNotEmpty = tempFolder.newFile("test/logs/not-empty-log-file.csv");
        FileUtils.write(logFileNotEmpty, "this;is;a;not;empty;log");

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        FileManagementTasklet archivalTasklet = new FileManagementTasklet();
        archivalTasklet.setHpanDirectory("file:/"+resolver.getResources(
            "classpath:/test-encrypt/**/hpan")[0].getFile().getAbsolutePath()+"/*.pgp");

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);

        archivalTasklet.execute(new StepContribution(execution),chunkContext);

        Collection<File> logFiles = FileUtils.listFiles(
            resolver.getResources("classpath:/test-encrypt/**/test/logs")[0].getFile(),
            new String[]{"csv"},false);

        assertThat(logFiles).isNotEmpty().hasSize(2);
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}