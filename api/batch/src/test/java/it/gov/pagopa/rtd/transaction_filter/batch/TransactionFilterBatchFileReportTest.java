package it.gov.pagopa.rtd.transaction_filter.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import it.gov.pagopa.rtd.transaction_filter.batch.config.TestConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.connector.FileReportRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.model.FileMetadata;
import it.gov.pagopa.rtd.transaction_filter.connector.model.FileReport;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBatchTest
@EnableAutoConfiguration
@DataJpaTest
@Transactional(propagation = NOT_SUPPORTED)
@Sql({
        "classpath:org/springframework/batch/core/schema-drop-hsqldb.sql",
        "classpath:org/springframework/batch/core/schema-hsqldb.sql" })
@ContextConfiguration(classes = {
        TestConfig.class,
        JacksonAutoConfiguration.class,
        TransactionFilterBatch.class,
        FeignAutoConfiguration.class
})
@TestPropertySource(properties = {
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
        "batchConfiguration.TransactionFilterBatch.successArchivePath=classpath:/test-encrypt/success",
        "batchConfiguration.TransactionFilterBatch.errorArchivePath=classpath:/test-encrypt/error",
        "batchConfiguration.TransactionFilterBatch.pendingArchivePath=classpath:/test-encrypt/output/pending",
        "batchConfiguration.TransactionFilterBatch.saltRecovery.enabled=false",
        "batchConfiguration.TransactionFilterBatch.pagopaPublicKeyRecovery.enabled=false",
        "batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled=false",
        "batchConfiguration.TransactionFilterBatch.abiToFiscalCodeMapRecovery.enabled=false",
        "batchConfiguration.TransactionFilterBatch.transactionSenderRtd.enabled=false",
        "batchConfiguration.TransactionFilterBatch.transactionSenderAde.enabled=false",
        "batchConfiguration.TransactionFilterBatch.transactionSenderPending.enabled=false",
        "batchConfiguration.TransactionFilterBatch.senderAdeAckFilesRecovery.enabled=false",
        "batchConfiguration.TransactionFilterBatch.senderAdeAckFilesRecovery.directoryPath=classpath:/test-encrypt/sender-ade-ack",
        "batchConfiguration.TransactionFilterBatch.fileReportRecovery.directoryPath=classpath:/test-encrypt/reports",
        "batchConfiguration.TransactionFilterBatch.fileReportRecovery.enabled=true",
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
})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TransactionFilterBatchFileReportTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @MockBean
    private FileReportRestClient fileReportRestClient;

    @SpyBean
    StoreService storeServiceSpy;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @SneakyThrows
    @Before
    public void setUp() {
        Mockito.reset(storeServiceSpy);

        deleteFiles("classpath:/test-encrypt/errorLogs/*.csv");
        deleteFiles("classpath:/test-encrypt/output/*.pgp");
        deleteFiles("classpath:/test-encrypt/sender-ade-ack/*.csv");
        deleteFiles("classpath:/test-encrypt/output/*.csv");
        deleteFiles("classpath:/test-encrypt/reports/*.json");
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

    @After
    public void cleanUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @SneakyThrows
    @Test
    public void jobExecutionProducesExpectedFiles() {
        LocalDateTime currentDate = LocalDateTime.now();
        String publicKey = createPublicKey();
        BDDMockito.doReturn(publicKey).when(storeServiceSpy).getKey("pagopa");
        BDDMockito.doReturn(getStubFileReport(currentDate)).when(fileReportRestClient).getFileReport();
        createPanPGP();

        File outputFileTrn = createTrnOutputFile();
        File outputFileAde = createAdeOutputFile();

        // Check that the job exited with the right exit status
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addDate("startDateTime", new Date())
                .toJobParameters());

        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        // Check that the HPAN store has been accessed as expected
        BDDMockito.verify(storeServiceSpy, times(3)).store(any());
        BDDMockito.verify(storeServiceSpy, times(5)).hasHpan(any());
        BDDMockito.verify(storeServiceSpy, times(2)).getKey(any());

        // Check that output folder contains expected files, and only those
        Collection<File> outputPgpFiles = getOutputPgpFiles();
        assertThat(outputPgpFiles).hasSize(2);

        Set<String> outputPgpFilenames = outputPgpFiles.stream().map(File::getName).collect(Collectors.toSet());
        Set<String> expectedPgpFilenames = getExpectedPgpFilenames();
        assertThat(expectedPgpFilenames).containsAll(outputPgpFilenames);

        Set<String> outputCsvFilenames = getOutputCsvFiles().stream().map(File::getName).collect(Collectors.toSet());
        Set<String> expectedCsvFilenames = getExpectedCsvFileNames();
        assertThat(outputCsvFilenames).containsAll(expectedCsvFilenames);

        List<String> outputFileTrnContent = Files.readAllLines(outputFileTrn.toPath().toAbsolutePath());
        List<String> outputFileAdeContent = Files.readAllLines(outputFileAde.toPath().toAbsolutePath());

        // Check that output files contain expected lines
        Set<String> expectedOutputFileTrnContent = getExpectedTrnOutputFileContent();
        Set<String> expectedOutputFileAdeContent = getExpectedAdeOutputFileContent();

        assertThat(expectedOutputFileTrnContent).containsAll(outputFileTrnContent);
        assertThat(outputFileTrnContent.get(0))
                .isEqualTo("#sha256sum:0632500c45de8ef75cd875f0898eaa886659519b615b968a81656e4405320a4d");
        assertThat(expectedOutputFileAdeContent).containsAll(outputFileAdeContent);
        assertThat(outputFileAdeContent.get(0))
                .isEqualTo("#sha256sum:0632500c45de8ef75cd875f0898eaa886659519b615b968a81656e4405320a4d");

        // Check that encrypted output files have the same content of unencrypted ones
        File trxEncFile = outputPgpFiles.stream()
                .filter(p -> p.getName().equals("CSTAR.99999.TRNLOG.20220204.094652.001.01.csv.pgp"))
                .collect(Collectors.toList()).iterator().next();

        FileInputStream trxEncFileIS = new FileInputStream(trxEncFile);
        FileInputStream secretFilePathIS = null;
        try {
            String secretKeyPath = "file:/" + this.getClass().getResource("/test-encrypt").getFile() + "/secretKey.asc";
            Resource secretKeyResource = resolver.getResource(secretKeyPath);

            secretFilePathIS = new FileInputStream(secretKeyResource.getFile());
            byte[] trxEncFileDecryptedFileData = EncryptUtil.decryptFile(trxEncFileIS, secretFilePathIS,
                    "test".toCharArray());
            File trxEncFileDecryptedFile = tempFolder.newFile("trxEncFileDecrypted.csv");
            FileUtils.writeByteArrayToFile(trxEncFileDecryptedFile, trxEncFileDecryptedFileData);

            List<String> trxEncFileDecryptedFileContent = Files
                    .readAllLines(trxEncFileDecryptedFile.toPath().toAbsolutePath());
            assertThat(expectedOutputFileTrnContent).containsAll(trxEncFileDecryptedFileContent);
        } finally {
            trxEncFileIS.close();
            secretFilePathIS.close();
        }

        // Check that logs folder contains expected files
        Collection<File> outputLogsFiles = FileUtils.listFiles(
                resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), new String[] { "csv" }, false);
        assertThat(outputLogsFiles).hasSize(2);

        FileFilter fileFilter = new WildcardFileFilter(
                "*_Rtd__FilteredRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> trxFilteredFiles = FileUtils.listFiles(
                resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter,
                null);
        assertThat(trxFilteredFiles).hasSize(1);

        fileFilter = new WildcardFileFilter("*_Ade__FilteredRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> adeFilteredFiles = FileUtils.listFiles(
                resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter,
                null);
        assertThat(adeFilteredFiles).hasSize(1);

        // empty log files get deleted
        fileFilter = new WildcardFileFilter("*_Rtd__ErrorRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> trxErrorFiles = FileUtils.listFiles(
                resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter,
                null);
        assertThat(trxErrorFiles).isEmpty();

        fileFilter = new WildcardFileFilter("*_Ade__ErrorRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> adeErrorFiles = FileUtils.listFiles(
                resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter,
                null);
        assertThat(adeErrorFiles).isEmpty();

        // Check that logs files contains expected lines
        File trxFilteredFile = trxFilteredFiles.iterator().next();
        List<String> trxFilteredContent = Files.readAllLines(trxFilteredFile.toPath().toAbsolutePath());
        assertThat(trxFilteredContent).hasSize(3)
                .contains(
                        "99999;00;01;pan4;03/20/2020 13:23:00;4444444444;8888;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;par4",
                        "99999;00;01;123456******3456;2020-03-20T13:23:00;555555555;9999;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;",
                        "99999;00;01;123456******3456;03/20/2020 15:23:00;666666666;9999;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;");

        File adeFilteredFile = adeFilteredFiles.iterator().next();
        List<String> adeFilteredContent = Files.readAllLines(adeFilteredFile.toPath().toAbsolutePath());
        assertThat(adeFilteredContent).hasSize(1)
                .contains(
                        "99999;00;01;123456******3456;2020-03-20T13:23:00;555555555;9999;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;");

        Collection<File> fileReportSaved = getFileReportSaved();
        assertThat(fileReportSaved).isNotNull().hasSize(1);

        List<String> fileReportContent = Files.readAllLines(fileReportSaved.stream().findAny()
                .orElse(new File("")).toPath());
        assertThat(fileReportContent).isNotNull().containsExactly("[",
                " {\"name\":\"file1\",\"size\":200,\"status\":\"RECEIVED\",\"transmissionDate\":\""
                        + currentDate.format(DATE_TIME_FORMATTER)
                        + "\",\"dataSummary\":{}},",
                " {\"name\":\"file2\",\"size\":300,\"status\":\"RECEIVED\",\"transmissionDate\":\""
                        + currentDate.minusDays(4).format(DATE_TIME_FORMATTER)
                        + "\",\"dataSummary\":{}},",
                " {\"name\":\"file3\",\"size\":400,\"status\":\"RECEIVED\",\"transmissionDate\":\""
                        + currentDate.minusDays(10).format(DATE_TIME_FORMATTER)
                        + "\",\"dataSummary\":{}}",
                "]");
    }

    @SneakyThrows
    @Test
    public void givenAReportWhenLaunchFileReportStepThenSaveTheReportOnFile() {
        LocalDateTime currentDate = LocalDateTime.now();
        BDDMockito.doReturn(getStubFileReport(currentDate)).when(fileReportRestClient).getFileReport();

        jobLauncherTestUtils.launchStep("file-report-recovery-step",
                new JobParameters());

        Mockito.verify(fileReportRestClient, times(1)).getFileReport();
        Collection<File> fileReportSaved = getFileReportSaved();

        assertThat(fileReportSaved).isNotNull().hasSize(1);

        List<String> fileReportContent = Files.readAllLines(fileReportSaved.stream().findAny()
                .orElse(new File("")).toPath());

        assertThat(fileReportContent).isNotNull().containsExactly("[",
                " {\"name\":\"file1\",\"size\":200,\"status\":\"RECEIVED\",\"transmissionDate\":\""
                        + currentDate.format(DATE_TIME_FORMATTER)
                        + "\",\"dataSummary\":{}},",
                " {\"name\":\"file2\",\"size\":300,\"status\":\"RECEIVED\",\"transmissionDate\":\""
                        + currentDate.minusDays(4)
                                .format(DATE_TIME_FORMATTER)
                        + "\",\"dataSummary\":{}},",

                " {\"name\":\"file3\",\"size\":400,\"status\":\"RECEIVED\",\"transmissionDate\":\""
                        + currentDate.minusDays(10)
                                .format(DATE_TIME_FORMATTER)
                        + "\",\"dataSummary\":{}}",

                "]");
    }

    @SneakyThrows
    @Test
    public void givenEmptyReportWhenLaunchFileReportStepThenSaveTheReportWithHeaderOnly() {
        BDDMockito.doReturn(getStubEmptyReport()).when(fileReportRestClient).getFileReport();

        jobLauncherTestUtils.launchStep("file-report-recovery-step",
                new JobParameters());

        Mockito.verify(fileReportRestClient, times(1)).getFileReport();
        Collection<File> fileReportSaved = getFileReportSaved();

        assertThat(fileReportSaved).isNotNull().hasSize(1);

        List<String> fileReportContent = Files.readAllLines(fileReportSaved.stream().findAny()
                .orElse(new File("")).toPath());

        assertThat(fileReportContent).isNotNull().contains("[", "]");
    }

    @SneakyThrows
    @Test
    public void givenMalformedReportWhenLaunchFileReportStepThenSaveTheReportWithHeaderOnly() {
        // returns report with null field list
        BDDMockito.doReturn(new FileReport()).when(fileReportRestClient).getFileReport();

        jobLauncherTestUtils.launchStep("file-report-recovery-step",
                new JobParameters());

        Mockito.verify(fileReportRestClient, times(1)).getFileReport();
        Collection<File> fileReportSaved = getFileReportSaved();

        assertThat(fileReportSaved).isNotNull().hasSize(1);

        List<String> fileReportContent = Files.readAllLines(fileReportSaved.stream().findAny()
                .orElse(new File("")).toPath());

        assertThat(fileReportContent).isNotNull().contains("[", "]");
    }

    @SneakyThrows
    private Collection<File> getFileReportSaved() {
        return FileUtils.listFiles(resolver.getResources("classpath:/test-encrypt/reports")[0].getFile(),
                new String[] { "json" }, false);
    }

    private FileReport getStubFileReport(LocalDateTime dateTime) {
        FileReport fileReport = new FileReport();
        List<FileMetadata> files = new ArrayList<>();

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setName("file1");
        fileMetadata.setSize(200L);
        fileMetadata.setTransmissionDate(dateTime);
        fileMetadata.setStatus("RECEIVED");
        files.add(fileMetadata);

        fileMetadata = new FileMetadata();
        fileMetadata.setName("file2");
        fileMetadata.setSize(300L);
        fileMetadata.setTransmissionDate(dateTime.minusDays(4));
        fileMetadata.setStatus("RECEIVED");
        files.add(fileMetadata);

        fileMetadata = new FileMetadata();
        fileMetadata.setName("file3");
        fileMetadata.setSize(400L);
        fileMetadata.setTransmissionDate(dateTime.minusDays(10));
        fileMetadata.setStatus("RECEIVED");
        files.add(fileMetadata);

        fileReport.setFilesRecentlyUploaded(files);

        return fileReport;
    }

    private FileReport getStubEmptyReport() {
        FileReport fileReport = new FileReport();
        fileReport.setFilesRecentlyUploaded(Collections.emptyList());

        return fileReport;
    }

    private String createPublicKey() throws IOException {
        String publicKeyPath = "file:/" + Objects.requireNonNull(
                this.getClass().getResource("/test-encrypt")).getFile() + "/publicKey.asc";
        Resource publicKeyResource = resolver.getResource(publicKeyPath);
        FileInputStream publicKeyFilePathIS = new FileInputStream(publicKeyResource.getFile());
        return IOUtils.toString(publicKeyFilePathIS);
    }

    @SneakyThrows
    private void createPanPGP() {
        tempFolder.newFolder("hpan");
        File panPgp = tempFolder.newFile("hpan/pan.pgp");

        FileOutputStream panPgpFOS = new FileOutputStream(panPgp);

        EncryptUtil.encryptFile(panPgpFOS,
                Objects.requireNonNull(this.getClass().getResource("/test-encrypt/pan")).getFile() + "/pan.csv",
                EncryptUtil.readPublicKey(
                        this.getClass().getResourceAsStream("/test-encrypt/publicKey.asc")),
                false, false);

        panPgpFOS.close();
    }

    @SneakyThrows
    private File createAdeOutputFile() {
        File outputFileAde = new File(resolver.getResource("classpath:/test-encrypt/output")
                .getFile().getAbsolutePath() + "/ADE.99999.TRNLOG.20220204.094652.001.01.csv");

        outputFileAde.createNewFile();
        return outputFileAde;
    }

    @SneakyThrows
    private File createTrnOutputFile() {
        File outputFileTrn = new File(resolver.getResource("classpath:/test-encrypt/output")
                .getFile().getAbsolutePath() + "/CSTAR.99999.TRNLOG.20220204.094652.001.01.csv");

        outputFileTrn.createNewFile();
        return outputFileTrn;
    }

    @SneakyThrows
    private Collection<File> getOutputPgpFiles() {
        return FileUtils.listFiles(
                resolver.getResources("classpath:/test-encrypt/output")[0].getFile(), new String[] { "pgp" }, false);
    }

    private Set<String> getExpectedPgpFilenames() {
        Set<String> expectedPgpFilenames = new HashSet<>();
        expectedPgpFilenames.add("CSTAR.99999.TRNLOG.20220204.094652.001.01.csv.pgp");
        expectedPgpFilenames.add("ADE.99999.TRNLOG.20220204.094652.001.01.csv.pgp");
        return expectedPgpFilenames;
    }

    @SneakyThrows
    private Collection<File> getOutputCsvFiles() {
        return FileUtils.listFiles(
                resolver.getResources("classpath:/test-encrypt/output")[0].getFile(), new String[] { "csv" }, false);
    }

    private Set<String> getExpectedCsvFileNames() {
        Set<String> expectedCsvFilenames = new HashSet<>();
        expectedCsvFilenames.add("CSTAR.99999.TRNLOG.20220204.094652.001.01.csv");
        expectedCsvFilenames.add("ADE.99999.TRNLOG.20220204.094652.001.01.csv");

        return expectedCsvFilenames;
    }

    private Set<String> getExpectedTrnOutputFileContent() {
        Set<String> expectedOutputFileTrnContent = new HashSet<>();
        expectedOutputFileTrnContent.add("#sha256sum:0632500c45de8ef75cd875f0898eaa886659519b615b968a81656e4405320a4d");
        expectedOutputFileTrnContent.add(
                "99999;00;00;a261f9479522020529213c5336dec371de5b3dacca0a8165c50ac33032c631ac;03/20/2020 10:50:33;1111111111;5555;;1111;978;22222;0000;1;000002;5422;fis123;12345678901;00;");
        expectedOutputFileTrnContent.add(
                "99999;00;01;e2df0a82ac0aa12921c398e1eba9119772db868650ebef22b8919fa0fb7642ed;03/20/2020 11:23:00;333333333;7777;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;");
        expectedOutputFileTrnContent.add(
                "99999;01;00;805f89015f85948f7d7bdd57a0a81e4cd95fc81bdd1195a69c4ab139f0ebed7b;03/20/2020 11:04:53;2222222222;6666;;2222;978;3333;0000;1;000002;5422;fis123;12345678901;00;par2");
        return expectedOutputFileTrnContent;
    }

    private Set<String> getExpectedAdeOutputFileContent() {
        String transmissionDate = getDateFormattedAsString();

        Set<String> expectedOutputFileAdeContent = new HashSet<>();
        expectedOutputFileAdeContent.add("#sha256sum:0632500c45de8ef75cd875f0898eaa886659519b615b968a81656e4405320a4d");
        expectedOutputFileAdeContent
                .add("99999;00;" + transmissionDate + ";03/20/2020;3;9999;978;4444;0000;1;fis123;12345678901;00");
        expectedOutputFileAdeContent
                .add("99999;01;" + transmissionDate + ";03/20/2020;1;2222;978;3333;0000;1;fis123;12345678901;00");
        expectedOutputFileAdeContent
                .add("99999;00;" + transmissionDate + ";03/20/2020;1;1111;978;22222;0000;1;fis123;12345678901;00");

        return expectedOutputFileAdeContent;
    }

    private String getDateFormattedAsString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return OffsetDateTime.now().format(fmt);
    }
}