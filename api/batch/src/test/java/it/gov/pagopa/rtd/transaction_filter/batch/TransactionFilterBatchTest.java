package it.gov.pagopa.rtd.transaction_filter.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import it.gov.pagopa.rtd.transaction_filter.batch.config.TestConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.connector.AbiToFiscalCodeRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SenderAdeAckRestClient;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterServiceImpl;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.After;
import org.junit.Assert;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ApplicationContext;
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
                "batchConfiguration.TransactionFilterBatch.successArchivePath=classpath:/test-encrypt/success",
                "batchConfiguration.TransactionFilterBatch.errorArchivePath=classpath:/test-encrypt/error",
                "batchConfiguration.TransactionFilterBatch.saltRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.pagopaPublicKeyRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.abiToFiscalCodeMapRecovery.enabled=true",
                "batchConfiguration.TransactionFilterBatch.transactionSenderRtd.enabled=false",
                "batchConfiguration.TransactionFilterBatch.transactionSenderAde.enabled=false",
                "batchConfiguration.TransactionFilterBatch.senderAdeAckFilesRecovery.enabled=true",
                "batchConfiguration.TransactionFilterBatch.senderAdeAckFilesRecovery.directoryPath=classpath:/test-encrypt/sender-ade-ack",
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
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.writerPoolSize=5"
        }
)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TransactionFilterBatchTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @MockBean
    private AbiToFiscalCodeRestClient abiToFiscalCodeRestClient;

    @MockBean
    private SenderAdeAckRestClient senderAdeAckRestClient;

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
    }

    @SneakyThrows
    @After
    public void tearDown() {
        deleteFiles("classpath:/test-encrypt/output/*.pgp");
        deleteFiles("classpath:/test-encrypt/sender-ade-ack/*.csv");

        tempFolder.delete();
    }

    @SneakyThrows
    private void deleteFiles(String classpath) {
        Resource[] resources = resolver.getResources(classpath);
        if (resources.length > 1) {
            for (Resource resource : resources) {
                resource.getFile().delete();
            }
        }
    }

    @After
    public void cleanUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @SneakyThrows
    @Test
    public void jobExecutionProducesExpectedFiles() {

        String publicKey = createPublicKey();

        BDDMockito.doReturn(publicKey).when(storeServiceSpy).getKey("pagopa");

        createPanPGP();

        File outputFileTrn = createTrnOutputFile();
        File outputFileAde = createAdeOutputFile();

        // Check that the job exited with the right exit status
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addDate("startDateTime", new Date())
                .toJobParameters());

        Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        // Check that the HPAN store has been accessed as expected
        BDDMockito.verify(storeServiceSpy, Mockito.times(3)).store(Mockito.any());
        BDDMockito.verify(storeServiceSpy, Mockito.times(4)).hasHpan(Mockito.any());
        BDDMockito.verify(storeServiceSpy, Mockito.times(2)).getKey(Mockito.any());

        // Check that output folder contains expected files, and only those
        Collection<File> outputPgpFiles = getOutputPgpFiles();
        Assert.assertEquals(2, outputPgpFiles.size());

        Set<String> outputPgpFilenames = outputPgpFiles.stream().map(File::getName).collect(Collectors.toSet());
        Set<String> expectedPgpFilenames = getExpectedPgpFilenames();
        Assert.assertEquals(expectedPgpFilenames, outputPgpFilenames);

        Set<String> outputCsvFilenames = getOutputCsvFiles().stream().map(File::getName).collect(Collectors.toSet());
        Set<String> expectedCsvFilenames = getExpectedCsvFileNames();
        assertThat(outputCsvFilenames).containsAll(expectedCsvFilenames);

        List<String> outputFileTrnContent = Files.readAllLines(outputFileTrn.toPath().toAbsolutePath());
        List<String> outputFileAdeContent = Files.readAllLines(outputFileAde.toPath().toAbsolutePath());

        // Check that output files contain expected lines
        Set<String> expectedOutputFileTrnContent = getExpectedTrnOutputFileContent();
        Set<String> expectedOutputFileAdeContent = getExpectedAdeOutputFileContent();

        Assert.assertEquals(expectedOutputFileTrnContent, new HashSet<>(outputFileTrnContent));
        Assert.assertEquals("#sha256sum:8bca0fdabf06e1c30b716224c67a5753ac5d999cf6a375ac7adba16f725f2046",
            outputFileTrnContent.get(0));
        Assert.assertEquals(expectedOutputFileAdeContent, new HashSet<>(outputFileAdeContent));
        Assert.assertEquals("#sha256sum:8bca0fdabf06e1c30b716224c67a5753ac5d999cf6a375ac7adba16f725f2046",
            outputFileAdeContent.get(0));

        // Check that encrypted output files have the same content of unencrypted ones
        File trxEncFile = outputPgpFiles.stream().filter(p -> p.getName().equals("CSTAR.99999.TRNLOG.20220204.094652.001.csv.pgp")).collect(Collectors.toList()).iterator().next();

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
            Assert.assertEquals(expectedOutputFileTrnContent, new HashSet<>(trxEncFileDecryptedFileContent));
        } finally {
            trxEncFileIS.close();
            secretFilePathIS.close();
        }

        // Check that logs folder contains expected files
        Collection<File> outputLogsFiles = FileUtils.listFiles(
                resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), new String[]{"csv"}, false);
        Assert.assertEquals(4, outputLogsFiles.size());

        FileFilter fileFilter = new WildcardFileFilter("*_Rtd__FilteredRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> trxFilteredFiles = FileUtils.listFiles(resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter, null);
        Assert.assertEquals(1, trxFilteredFiles.size());

        fileFilter = new WildcardFileFilter("*_Ade__FilteredRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> adeFilteredFiles = FileUtils.listFiles(resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter, null);
        Assert.assertEquals(1, adeFilteredFiles.size());

        fileFilter = new WildcardFileFilter("*_Rtd__ErrorRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> trxErrorFiles = FileUtils.listFiles(resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter, null);
        Assert.assertEquals(1, trxErrorFiles.size());

        fileFilter = new WildcardFileFilter("*_Ade__ErrorRecords_CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        Collection<File> adeErrorFiles = FileUtils.listFiles(resolver.getResources("classpath:/test-encrypt/errorLogs")[0].getFile(), (IOFileFilter) fileFilter, null);
        Assert.assertEquals(1, adeErrorFiles.size());

        // Check that logs files contains expected lines
        File trxFilteredFile = trxFilteredFiles.iterator().next();
        List<String> trxFilteredContent = Files.readAllLines(trxFilteredFile.toPath().toAbsolutePath());
        Assert.assertEquals(2, trxFilteredContent.size());
        Assert.assertTrue(trxFilteredContent.contains("99999;00;01;pan4;03/20/2020 13:23:00;4444444444;8888;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;par4"));
        Assert.assertTrue(trxFilteredContent.contains("99999;00;01;pan5;2020-03-20T13:23:00;555555555;9999;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;"));

        File adeFilteredFile = adeFilteredFiles.iterator().next();
        List<String> adeFilteredContent = Files.readAllLines(adeFilteredFile.toPath().toAbsolutePath());
        Assert.assertEquals(1, adeFilteredContent.size());
        Assert.assertTrue(adeFilteredContent.contains("99999;00;01;pan5;2020-03-20T13:23:00;555555555;9999;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;"));

        File trxErrorFile = trxErrorFiles.iterator().next();
        List<String> trxErrorContent = Files.readAllLines(trxErrorFile.toPath().toAbsolutePath());
        Assert.assertEquals(0, trxErrorContent.size());

        File adeErrorFile = adeErrorFiles.iterator().next();
        List<String> adeErrorContent = Files.readAllLines(adeErrorFile.toPath().toAbsolutePath());
        Assert.assertEquals(0, adeErrorContent.size());
    }

    @SneakyThrows
    @Test
    public void whenAbiToFiscalCodeMapIsSetThenOutputMustHaveConvertedAcquirerId() {
        String publicKey = createPublicKey();
        createPanPGP();
        createTrnOutputFile();
        File outputFileAde = createAdeOutputFile();
        BDDMockito.doReturn(publicKey).when(storeServiceSpy).getKey("pagopa");
        BDDMockito.doReturn(createAbiToFiscalCodeMap()).when(abiToFiscalCodeRestClient)
            .getFakeAbiToFiscalCodeMap();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
            .addDate("startDateTime", new Date())
            .toJobParameters());
        Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        List<String> outputFileAdeContent = Files.readAllLines(outputFileAde.toPath().toAbsolutePath());
        Set<String> expectedOutputFileAdeContent = getExpectedAdeOutputFileContentWithAbiConverted();

        Assert.assertEquals(expectedOutputFileAdeContent, new HashSet<>(outputFileAdeContent));
    }

    @SneakyThrows
    @Test
    public void whenSenderAdeAckTaskletIsSetThenMatchExpectedOutputFiles() {
        String publicKey = createPublicKey();
        createPanPGP();
        createTrnOutputFile();
        createAdeOutputFile();
        BDDMockito.doReturn(publicKey).when(storeServiceSpy).getKey("pagopa");
        BDDMockito.doReturn(createSenderAdeAckFiles()).when(senderAdeAckRestClient)
            .getSenderAdeAckFiles();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
            .addDate("startDateTime", new Date())
            .toJobParameters());
        Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        Collection<File> filesSaved = getSenderAdeAckFilesSavedByTasklet();
        assertThat(filesSaved).isNotNull().isNotEmpty();
        assertThat(filesSaved.stream()
            .filter(file -> "CSTAR.STPAY.ADEACK.20220801.211940.001.csv".equals(file.getName()))
            .findAny().orElse(null))
            .hasContent("12345;fiscalcode");
        assertThat(filesSaved.stream()
            .filter(file -> "senderAdeAck2.csv".equals(file.getName()))
            .findAny().orElse(null))
            .hasContent("7890123;stringdefault");
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

    private Map<String, String> createAbiToFiscalCodeMap() {
        Map<String, String> abiToFiscalCodeMap = new HashMap<>();
        abiToFiscalCodeMap.put("4444", "fiscalCode");
        return abiToFiscalCodeMap;
    }

    @SneakyThrows
    private Collection<File> getSenderAdeAckFilesSavedByTasklet() {
        return FileUtils.listFiles(resolver.getResources("classpath:/test-encrypt/sender-ade-ack")[0].getFile(), new String[]{"csv"}, false);
    }

    @SneakyThrows
    private File createAdeOutputFile() {
        File outputFileAde = new File(resolver.getResource("classpath:/test-encrypt/output")
            .getFile().getAbsolutePath() + "/ADE.99999.TRNLOG.20220204.094652.001.csv");

        outputFileAde.createNewFile();
        return outputFileAde;
    }

    @SneakyThrows
    private File createTrnOutputFile() {
        File outputFileTrn = new File(resolver.getResource("classpath:/test-encrypt/output")
            .getFile().getAbsolutePath() + "/CSTAR.99999.TRNLOG.20220204.094652.001.csv");

        outputFileTrn.createNewFile();
        return outputFileTrn;
    }

    @SneakyThrows
    private Collection<File> getOutputPgpFiles() {
        return FileUtils.listFiles(
            resolver.getResources("classpath:/test-encrypt/output")[0].getFile(), new String[]{"pgp"}, false);
    }

    private Set<String> getExpectedPgpFilenames() {
        Set<String> expectedPgpFilenames = new HashSet<>();
        expectedPgpFilenames.add("CSTAR.99999.TRNLOG.20220204.094652.001.csv.pgp");
        expectedPgpFilenames.add("ADE.99999.TRNLOG.20220204.094652.001.csv.pgp");
        return expectedPgpFilenames;
    }

    @SneakyThrows
    private Collection<File> getOutputCsvFiles() {
        return FileUtils.listFiles(
            resolver.getResources("classpath:/test-encrypt/output")[0].getFile(), new String[]{"csv"}, false);
    }

    private Set<String> getExpectedCsvFileNames() {
        Set<String> expectedCsvFilenames = new HashSet<>();
        expectedCsvFilenames.add("CSTAR.99999.TRNLOG.20220204.094652.001.csv");
        expectedCsvFilenames.add("ADE.99999.TRNLOG.20220204.094652.001.csv");

        return expectedCsvFilenames;
    }

    private Set<String> getExpectedTrnOutputFileContent() {
        Set<String> expectedOutputFileTrnContent = new HashSet<>();
        expectedOutputFileTrnContent.add("#sha256sum:8bca0fdabf06e1c30b716224c67a5753ac5d999cf6a375ac7adba16f725f2046");
        expectedOutputFileTrnContent.add("99999;00;00;28aa47c8c6cd1a6b0a86ebe18471295796c88269868825b4cd41f94f0a07e88e;03/20/2020 10:50:33;1111111111;5555;;1111;978;22222;0000;1;000002;5422;fis123;12345678901;00;");
        expectedOutputFileTrnContent.add("99999;00;01;e2df0a82ac0aa12921c398e1eba9119772db868650ebef22b8919fa0fb7642ed;03/20/2020 11:23:00;333333333;7777;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;");
        expectedOutputFileTrnContent.add("99999;01;00;805f89015f85948f7d7bdd57a0a81e4cd95fc81bdd1195a69c4ab139f0ebed7b;03/20/2020 11:04:53;2222222222;6666;;2222;978;3333;0000;1;000002;5422;fis123;12345678901;00;par2");
        return expectedOutputFileTrnContent;
    }

    private Set<String> getExpectedAdeOutputFileContentWithAbiConverted() {
        String transmissionDate = getDateFormattedAsString();

        Set<String> expectedOutputFileAdeContent = new HashSet<>();
        expectedOutputFileAdeContent.add("#sha256sum:8bca0fdabf06e1c30b716224c67a5753ac5d999cf6a375ac7adba16f725f2046");
        expectedOutputFileAdeContent.add("99999;00;" + transmissionDate + ";03/20/2020;2;6666;978;fiscalCode;0000;1;fis123;12345678901;00");
        expectedOutputFileAdeContent.add("99999;01;" + transmissionDate + ";03/20/2020;1;2222;978;3333;0000;1;fis123;12345678901;00");
        expectedOutputFileAdeContent.add("99999;00;" + transmissionDate + ";03/20/2020;1;1111;978;22222;0000;1;fis123;12345678901;00");

        return expectedOutputFileAdeContent;
    }

    private Set<String> getExpectedAdeOutputFileContent() {
        String transmissionDate = getDateFormattedAsString();

        Set<String> expectedOutputFileAdeContent = new HashSet<>();
        expectedOutputFileAdeContent.add("#sha256sum:8bca0fdabf06e1c30b716224c67a5753ac5d999cf6a375ac7adba16f725f2046");
        expectedOutputFileAdeContent.add("99999;00;" + transmissionDate + ";03/20/2020;2;6666;978;4444;0000;1;fis123;12345678901;00");
        expectedOutputFileAdeContent.add("99999;01;" + transmissionDate + ";03/20/2020;1;2222;978;3333;0000;1;fis123;12345678901;00");
        expectedOutputFileAdeContent.add("99999;00;" + transmissionDate + ";03/20/2020;1;1111;978;22222;0000;1;fis123;12345678901;00");

        return expectedOutputFileAdeContent;
    }
    private String getDateFormattedAsString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return OffsetDateTime.now().format(fmt);
    }

    @SneakyThrows
    List<File> createSenderAdeAckFiles() {
        List<File> files = new ArrayList<>();
        tempFolder.newFolder("temporarySenderAdeAck");

        File firstFile = tempFolder.newFile("temporarySenderAdeAck/CSTAR.STPAY.ADEACK.20220801.211940.001.csv");
        byte[] firstFileContent = "12345;fiscalcode".getBytes(StandardCharsets.UTF_8);
        Files.write(firstFile.toPath(), firstFileContent);
        files.add(firstFile);

        File secondFile = tempFolder.newFile("temporarySenderAdeAck/senderAdeAck2.csv");
        byte[] secondFileContent = "7890123;stringdefault".getBytes(StandardCharsets.UTF_8);
        Files.write(secondFile.toPath(), secondFileContent);
        files.add(secondFile);

        files.forEach(file -> System.out.println(file.getAbsolutePath()));
        return files;
    }
}