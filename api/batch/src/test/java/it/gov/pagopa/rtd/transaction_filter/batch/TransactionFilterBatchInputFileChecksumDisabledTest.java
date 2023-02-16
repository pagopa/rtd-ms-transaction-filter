package it.gov.pagopa.rtd.transaction_filter.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import it.gov.pagopa.rtd.transaction_filter.batch.config.TestConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
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
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import wiremock.com.google.common.collect.Sets;

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
                "batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath=classpath:/test-encrypt/**/transactions/",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath=classpath:/test-encrypt/output",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.transactionLogsPath=classpath:/test-encrypt/errorLogs",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.applyHashing=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.timestampPattern=MM/dd/yyyy HH:mm:ss",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.applyEncrypt=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.saveHashing=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.deleteProcessedFiles=false",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.deleteOutputFiles=ERROR",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.inputFileChecksumEnabled=false",
                "batchConfiguration.TransactionFilterBatch.successArchivePath=classpath:/test-encrypt/success",
                "batchConfiguration.TransactionFilterBatch.errorArchivePath=classpath:/test-encrypt/error",
                "batchConfiguration.TransactionFilterBatch.saltRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.pagopaPublicKeyRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.abiToFiscalCodeMapRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.transactionSenderRtd.enabled=false",
                "batchConfiguration.TransactionFilterBatch.transactionSenderAde.enabled=false",
                "batchConfiguration.TransactionFilterBatch.senderAdeAckFilesRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.fileReportRecovery.directoryPath=classpath:/test-encrypt/reports",
                "batchConfiguration.TransactionFilterBatch.fileReportRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessFileLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnReadErrorLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnReadErrorFileLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessFileLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnProcessErrorLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnProcessErrorFileLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnWriteErrorLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnWriteErrorFileLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.loggingFrequency=100",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.writerPoolSize=5",
                "batchConfiguration.TransactionFilterBatch.transactionWriterAde.splitThreshold=1000"
        }
)
public class TransactionFilterBatchInputFileChecksumDisabledTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @SpyBean
    StoreService storeServiceSpy;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(new File(getClass().getResource("/test-encrypt").getFile()));

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @SneakyThrows
    @Before
    public void setUp() {
        Mockito.reset(storeServiceSpy);

        deleteFiles("classpath:/test-encrypt/errorLogs/*.csv");
        deleteFiles("classpath:/test-encrypt/output/*.pgp");
        deleteFiles("classpath:/test-encrypt/output/*.csv");
    }

    @SneakyThrows
    @After
    public void tearDown() {
        tempFolder.delete();
    }

    @SneakyThrows
    private void deleteFiles(String classpath) {
        Resource[] resources = resolver.getResources(classpath);
        for (Resource resource : resources) {
            resource.getFile().delete();
        }
    }

    @SneakyThrows
    @Test
    public void jobExecutionProducesExpectedFiles() {

        String publicKeyPath = "file:/" + this.getClass().getResource("/test-encrypt").getFile() + "/publicKey.asc";
        Resource publicKeyResource = resolver.getResource(publicKeyPath);
        FileInputStream publicKeyFilePathIS = new FileInputStream(publicKeyResource.getFile());
        String publicKey = IOUtils.toString(publicKeyFilePathIS);

        BDDMockito.doReturn(publicKey).when(storeServiceSpy).getKey("pagopa");

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
                .getFile().getAbsolutePath() + "/CSTAR.99999.TRNLOG.20220204.094652.001.01.csv");

        if (!outputFileTrn.exists()) {
            outputFileTrn.createNewFile();
        }

        File outputFileAde = new File(resolver.getResource("classpath:/test-encrypt/output")
            .getFile().getAbsolutePath() + "/ADE.99999.TRNLOG.20220204.094652.001.01.csv");

        if (!outputFileAde.exists()) {
            outputFileAde.createNewFile();
        }

        // Check that the job exited with the right exit status
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addDate("startDateTime", new Date())
                .toJobParameters());

        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // Check that the HPAN store has been accessed as expected
        BDDMockito.verify(storeServiceSpy, Mockito.times(3)).store(Mockito.any());
        BDDMockito.verify(storeServiceSpy, Mockito.times(5)).hasHpan(Mockito.any());
        BDDMockito.verify(storeServiceSpy, Mockito.times(2)).getKey(Mockito.any());

        // Check that output folder contains expected files, and only those
        Collection<File> outputPgpFiles = FileUtils.listFiles(
                resolver.getResources("classpath:/test-encrypt/output")[0].getFile(), new String[]{"pgp"}, false);
        assertThat(outputPgpFiles).hasSize(2);

        Set<String> outputPgpFilenames = outputPgpFiles.stream().map(File::getName).collect(Collectors.toSet());
        Set<String> expectedPgpFilenames = new HashSet<>();
        expectedPgpFilenames.add("CSTAR.99999.TRNLOG.20220204.094652.001.01.csv.pgp");
        expectedPgpFilenames.add("ADE.99999.TRNLOG.20220204.094652.001.01.csv.pgp");
        assertThat(expectedPgpFilenames).containsAll(outputPgpFilenames);

        Collection<String> outputCsvFilenames = FileUtils.listFiles(
            resolver.getResources("classpath:/test-encrypt/output")[0].getFile(), new String[]{"csv"}, false)
                .stream().map(File::getName).collect(Collectors.toSet());
        Set<String> expectedCsvFilenames = getExpectedCsvFileNames();
        assertThat(outputCsvFilenames).containsAll(expectedCsvFilenames);

        List<String> outputFileTrnContent = Files.readAllLines(outputFileTrn.toPath().toAbsolutePath());
        List<String> outputFileAdeContent = Files.readAllLines(outputFileAde.toPath().toAbsolutePath());

        // Check that output files contain expected lines
        Set<String> expectedOutputFileTrnContent = new HashSet<>();
        expectedOutputFileTrnContent.add("99999;00;00;a261f9479522020529213c5336dec371de5b3dacca0a8165c50ac33032c631ac;03/20/2020 10:50:33;1111111111;5555;;1111;978;22222;0000;1;000002;5422;fis123;12345678901;00;");
        expectedOutputFileTrnContent.add("99999;00;01;e2df0a82ac0aa12921c398e1eba9119772db868650ebef22b8919fa0fb7642ed;03/20/2020 11:23:00;333333333;7777;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;");
        expectedOutputFileTrnContent.add("99999;01;00;805f89015f85948f7d7bdd57a0a81e4cd95fc81bdd1195a69c4ab139f0ebed7b;03/20/2020 11:04:53;2222222222;6666;;2222;978;3333;0000;1;000002;5422;fis123;12345678901;00;par2");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String transmissionDate = OffsetDateTime.now().format(fmt);

        Set<String> expectedOutputFileAdeContent = new HashSet<>();
        expectedOutputFileAdeContent.add("99999;00;" + transmissionDate + ";03/20/2020;3;9999;978;4444;0000;1;fis123;12345678901;00");
        expectedOutputFileAdeContent.add("99999;01;" + transmissionDate + ";03/20/2020;1;2222;978;3333;0000;1;fis123;12345678901;00");
        expectedOutputFileAdeContent.add("99999;00;" + transmissionDate + ";03/20/2020;1;1111;978;22222;0000;1;fis123;12345678901;00");

        assertThat(expectedOutputFileTrnContent).containsAll(outputFileTrnContent);
        assertThat(expectedOutputFileAdeContent).containsAll(outputFileAdeContent);

        // Check that encrypted output files have the same content of unencrypted ones
        File trxEncFile = outputPgpFiles.stream().filter(p -> p.getName().equals("CSTAR.99999.TRNLOG.20220204.094652.001.01.csv.pgp")).collect(Collectors.toList()).iterator().next();

        FileInputStream trxEncFileIS = new FileInputStream(trxEncFile);
        FileInputStream secretFilePathIS = null;
        try {
            String secretKeyPath = "file:/" + this.getClass().getResource("/test-encrypt").getFile() + "/secretKey.asc";
            Resource secretKeyResource = resolver.getResource(secretKeyPath);

            secretFilePathIS = new FileInputStream(secretKeyResource.getFile());
            byte[] trxEncFileDecryptedFileData = EncryptUtil.decryptFile(trxEncFileIS, secretFilePathIS, "test".toCharArray());
            File trxEncFileDecryptedFile = tempFolder.newFile("trxEncFileDecrypted.csv");
            FileUtils.writeByteArrayToFile(trxEncFileDecryptedFile, trxEncFileDecryptedFileData);

            List<String> trxEncFileDecryptedFileContent = Files.readAllLines(trxEncFileDecryptedFile.toPath().toAbsolutePath());
            assertThat(expectedOutputFileTrnContent).containsAll(trxEncFileDecryptedFileContent);
        } finally {
            trxEncFileIS.close();
            secretFilePathIS.close();
        }

        // Check that logs folder contains expected files
        Collection<File> outputLogsFiles = FileUtils.listFiles(
                resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), new String[]{"csv"}, false);
        assertThat(outputLogsFiles).hasSize(2);

        FileFilter fileFilter = new WildcardFileFilter("*_Rtd__FilteredRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> trxFilteredFiles = FileUtils.listFiles(resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter, null);
        assertThat(trxFilteredFiles).hasSize(1);

        fileFilter = new WildcardFileFilter("*_Ade__FilteredRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> adeFilteredFiles = FileUtils.listFiles(resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter, null);
        assertThat(adeFilteredFiles).hasSize(1);

        // empty log files get deleted
        fileFilter = new WildcardFileFilter("*_Rtd__ErrorRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> trxErrorFiles = FileUtils.listFiles(resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter, null);
        assertThat(trxErrorFiles).isEmpty();

        fileFilter = new WildcardFileFilter("*_Ade__ErrorRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> adeErrorFiles = FileUtils.listFiles(resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter, null);
        assertThat(adeErrorFiles).isEmpty();

        // Check that logs files contains expected lines
        File trxFilteredFile = trxFilteredFiles.iterator().next();
        List<String> trxFilteredContent = Files.readAllLines(trxFilteredFile.toPath().toAbsolutePath());
        assertThat(trxFilteredContent).hasSize(3)
            .contains("99999;00;01;pan4;03/20/2020 13:23:00;4444444444;8888;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;par4",
                "99999;00;01;123456******3456;2020-03-20T13:23:00;555555555;9999;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;",
                "99999;00;01;123456******3456;03/20/2020 15:23:00;666666666;9999;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;");

        File adeFilteredFile = adeFilteredFiles.iterator().next();
        List<String> adeFilteredContent = Files.readAllLines(adeFilteredFile.toPath().toAbsolutePath());
        assertThat(adeFilteredContent).hasSize(1)
            .contains("99999;00;01;123456******3456;2020-03-20T13:23:00;555555555;9999;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;");
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
        BDDMockito.verify(storeServiceSpy, Mockito.times(0)).store(Mockito.any());

    }

    private Set<String> getExpectedCsvFileNames() {
        return Sets.newHashSet("CSTAR.99999.TRNLOG.20220204.094652.001.01.csv", "ADE.99999.TRNLOG.20220204.094652.001.01.csv");
    }
}