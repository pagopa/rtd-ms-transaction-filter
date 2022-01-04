package it.gov.pagopa.rtd.transaction_filter.batch;

import it.gov.pagopa.rtd.transaction_filter.batch.config.TestConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Assert;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep.ADE_OUTPUT_FILE_PREFIX;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@RunWith(SpringRunner.class)
@SpringBatchTest
@EnableAutoConfiguration
@DataJpaTest
@Transactional(propagation = NOT_SUPPORTED)
@Sql({
        "classpath:org/springframework/batch/core/schema-drop-hsqldb.sql",
        "classpath:org/springframework/batch/core/schema-hsqldb.sql"})
@ContextConfiguration(classes = {
        TestConfig.class,
        JacksonAutoConfiguration.class,
        TransactionFilterBatch.class,
        FeignAutoConfiguration.class
})
@TestPropertySource(
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "batchConfiguration.TransactionFilterBatch.panList.secretKeyPath=classpath:/test-encrypt/secretKey.asc",
                "batchConfiguration.TransactionFilterBatch.panList.passphrase=test",
                "batchConfiguration.TransactionFilterBatch.panList.skipLimit=0",
                "batchConfiguration.TransactionFilterBatch.panList.hpanDirectoryPath=classpath:/test-encrypt/**/hpan/*pan*.pgp",
                "batchConfiguration.TransactionFilterBatch.panList.linesToSkip=0",
                "batchConfiguration.TransactionFilterBatch.panList.applyDecrypt=true",
                "batchConfiguration.TransactionFilterBatch.panList.applyHashing=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath=classpath:/test-encrypt/**/transactions/*trx*.csv",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath=classpath:/test-encrypt/output",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.publicKeyPath=classpath:/test-encrypt/publicKey.asc",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.applyHashing=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.timestampPattern=MM/dd/yyyy HH:mm:ss",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.applyEncrypt=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.saveHashing=true",
                "batchConfiguration.CsvTransactionReaderBatch.successArchivePath=classpath:/test-encrypt/**/success",
                "batchConfiguration.CsvTransactionReaderBatch.errorArchivePath=classpath:/test-encrypt/**/error",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.deleteLocalFiles=false",
                "batchConfiguration.TransactionFilterBatch.saltRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.transactionSender.enabled=false",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessFileLogging=false"
        }
)
public class TransactionFilterBatchTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @SpyBean
    HpanStoreService hpanStoreServiceSpy;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @SneakyThrows
    @Before
    public void setUp() {
        Mockito.reset(hpanStoreServiceSpy);
    }

    @SneakyThrows
    @After
    public void tearDown() {
        Resource[] resources = resolver.getResources("classpath:/test-encrypt/output/*.pgp");
        if (resources.length > 1) {
            for (Resource resource : resources) {
                resource.getFile().delete();
            }
        }
        tempFolder.delete();
    }

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private JobParameters defaultJobParameters() {
        return new JobParametersBuilder()
                .addDate("startDateTime", new Date())
                .toJobParameters();
    }

    @SneakyThrows
    @Test
    public void jobExecutionProducesExpectedFiles() {

        tempFolder.newFolder("hpan");
        File panPgp = tempFolder.newFile("hpan/pan.pgp");

        FileOutputStream panPgpFOS = new FileOutputStream(panPgp);

        EncryptUtil.encryptFile(panPgpFOS,
                this.getClass().getResource("/test-encrypt/pan").getFile() + "/pan.csv",
                EncryptUtil.readPublicKey(
                        this.getClass().getResourceAsStream("/test-encrypt/publicKey.asc")),
                false, false);

        panPgpFOS.close();

        File outputFileTrn = new File(resolver.getResource("classpath:/test-encrypt/output")
                .getFile().getAbsolutePath() + "/test-trx.csv");
        File outputFileAde = new File(resolver.getResource("classpath:/test-encrypt/output")
                .getFile().getAbsolutePath() + "/" + ADE_OUTPUT_FILE_PREFIX + "test-trx.csv");

        if (!outputFileTrn.exists()) {
            outputFileTrn.createNewFile();
        }
        if (!outputFileAde.exists()) {
            outputFileAde.createNewFile();
        }

        // Check that the job exited with the right exit status
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(defaultJobParameters());
        Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        // Check that the HPAN store has been accessed as expected
        BDDMockito.verify(hpanStoreServiceSpy, Mockito.times(3)).store(Mockito.any());
        BDDMockito.verify(hpanStoreServiceSpy, Mockito.times(4)).hasHpan(Mockito.any());

        // Check that output folder contains expected files, and only those
        Collection<File> outputPgpFiles = FileUtils.listFiles(
                resolver.getResources("classpath:/test-encrypt/output")[0].getFile(), new String[]{"pgp"}, false);
        Assert.assertEquals(2, outputPgpFiles.size());

        List<String> outputPgpFilenames = outputPgpFiles.stream().map(p -> p.getName()).collect(Collectors.toList());
        List<String> expectedPgpFilenames = new ArrayList<>();
        expectedPgpFilenames.add("test-trx.csv.pgp");
        expectedPgpFilenames.add("ADE.test-trx.csv.pgp");
        Assert.assertEquals(expectedPgpFilenames, outputPgpFilenames);

        Collection<File> outputCsvFiles = FileUtils.listFiles(
            resolver.getResources("classpath:/test-encrypt/output")[0].getFile(), new String[]{"csv"}, false);
        Assert.assertEquals(2, outputCsvFiles.size());

        List<String> outputCsvFilenames = outputCsvFiles.stream().map(p -> p.getName()).collect(Collectors.toList());
        List<String> expectedCsvFilenames = new ArrayList<>();
        expectedCsvFilenames.add("test-trx.csv");
        expectedCsvFilenames.add("ADE.test-trx.csv");
        Assert.assertEquals(expectedCsvFilenames, outputCsvFilenames);

        List<String> outputFileTrnContent = Files.readAllLines(outputFileTrn.toPath().toAbsolutePath());
        List<String> outputFileAdeContent = Files.readAllLines(outputFileAde.toPath().toAbsolutePath());

        // Check that output files contains the expected number of lines
        Assert.assertEquals(3, outputFileTrnContent.size());
        Assert.assertEquals(4, outputFileAdeContent.size());

        // Check that output files contains expected lines
        Assert.assertTrue(outputFileTrnContent.contains("13131;00;00;28aa47c8c6cd1a6b0a86ebe18471295796c88269868825b4cd41f94f0a07e88e;03/20/2020 10:50:33;1111111111;5555;;1111;896;22222;0000;1;000002;5422;fis123;12345678901;00;"));
        Assert.assertTrue(outputFileTrnContent.contains("131331;00;01;e2df0a82ac0aa12921c398e1eba9119772db868650ebef22b8919fa0fb7642ed;03/20/2020 11:23:00;333333333;7777;;3333;896;4444;0000;1;000002;5422;fis123;12345678901;00;"));
        Assert.assertTrue(outputFileTrnContent.contains("13131;01;00;805f89015f85948f7d7bdd57a0a81e4cd95fc81bdd1195a69c4ab139f0ebed7b;03/20/2020 11:04:53;2222222222;6666;;2222;896;3333;0000;1;000002;5422;fis123;12345678901;00;"));

        Assert.assertTrue(outputFileAdeContent.contains("13131;00;00;03/20/2020 10:50:33;1111111111;5555;;1111;896;22222;0000;1;000002;5422;fis123;12345678901;00;"));
        Assert.assertTrue(outputFileAdeContent.contains("13131;01;00;03/20/2020 11:04:53;2222222222;6666;;2222;896;3333;0000;1;000002;5422;fis123;12345678901;00;"));
        Assert.assertTrue(outputFileAdeContent.contains("131331;00;01;03/20/2020 11:23:00;333333333;7777;;3333;896;4444;0000;1;000002;5422;fis123;12345678901;00;"));
        Assert.assertTrue(outputFileAdeContent.contains("131331;00;01;03/20/2020 13:23:00;4444444444;8888;;3333;896;4444;0000;1;000002;5422;fis123;12345678901;00;"));
    }

    @SneakyThrows
    @Test
    public void jobExecutionFails() {

        tempFolder.newFolder("hpan");
        File panPgp = tempFolder.newFile("hpan/pan.pgp");

        FileOutputStream panPgpFOS = new FileOutputStream(panPgp);

        EncryptUtil.encryptFile(panPgpFOS,
                this.getClass().getResource("/test-encrypt/pan").getFile() + "/pan.csv",
                EncryptUtil.readPublicKey(
                        this.getClass().getResourceAsStream("/test-encrypt/otherPublicKey.asc")),
                false, false);

        panPgpFOS.close();

        jobLauncherTestUtils.launchStep("hpan-recovery-master-step");
        BDDMockito.verify(hpanStoreServiceSpy, Mockito.times(0)).store(Mockito.any());

    }
}