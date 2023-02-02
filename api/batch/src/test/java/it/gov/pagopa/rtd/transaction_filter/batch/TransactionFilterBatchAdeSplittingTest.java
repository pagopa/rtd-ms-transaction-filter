package it.gov.pagopa.rtd.transaction_filter.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

import it.gov.pagopa.rtd.transaction_filter.batch.config.TestConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
        "batchConfiguration.TransactionFilterBatch.transactionWriterAde.splitThreshold=2",
        "batchConfiguration.TransactionFilterBatch.transactionFilter.chunkSize=2"
    }
)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class TransactionFilterBatchAdeSplittingTest {

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  private JobRepositoryTestUtils jobRepositoryTestUtils;

  @SpyBean
  StoreService storeServiceSpy;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder(
      new File(getClass().getResource("/test-encrypt").getFile()));

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

  @After
  public void cleanUp() {
    jobRepositoryTestUtils.removeJobExecutions();
  }

  /**
   * Important: the chunk size and the ade split threshold must be equals or multiply e.g. (2,2) or
   * (50,100) Otherwise we read (and write) a chunk bigger than the threshold itself
   */
  @SneakyThrows
  @Test
  public void jobExecutionProducesExpectedFiles() {
    String publicKey = createPublicKey();
    BDDMockito.doReturn(publicKey).when(storeServiceSpy).getKey("pagopa");
    createPanPGP();

    File outputFileTrn = createTrnOutputFile();
    Set<File> outputFilesAde = createAdeOutputFiles();

    // Check that the job exited with the right exit status
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
        .addDate("startDateTime", new Date())
        .toJobParameters());

    Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

    // Check that the HPAN store has been accessed as expected
    BDDMockito.verify(storeServiceSpy, times(3)).store(any());
    BDDMockito.verify(storeServiceSpy, times(4)).hasHpan(any());
    BDDMockito.verify(storeServiceSpy, times(3)).getKey(any());

    // Check that output folder contains expected files, and only those
    Collection<File> outputPgpFiles = getOutputPgpFiles();
    Assert.assertEquals(3, outputPgpFiles.size());

    Set<String> outputPgpFilenames = outputPgpFiles.stream().map(File::getName)
        .collect(Collectors.toSet());
    Set<String> expectedPgpFilenames = getExpectedPgpFilenames();
    Assert.assertEquals(expectedPgpFilenames, outputPgpFilenames);

    Set<String> outputCsvFilenames = getOutputCsvFiles().stream().map(File::getName)
        .collect(Collectors.toSet());
    Set<String> expectedCsvFilenames = getExpectedCsvFileNames();
    assertThat(outputCsvFilenames).containsAll(expectedCsvFilenames);

    List<String> outputFileTrnContent = Files.readAllLines(outputFileTrn.toPath().toAbsolutePath());
    List<String> outputFirstChunkAdeContent = Files.readAllLines(Objects.requireNonNull(
        outputFilesAde.stream().filter(file -> file.getName().contains("01.csv")).map(File::toPath)
            .findFirst().orElse(null)));
    // 2 aggregates records which corresponds to split threshold plus the checksum header
    assertThat(outputFirstChunkAdeContent).hasSize(3);

    List<String> outputSecondChunkAdeContent = Files.readAllLines(Objects.requireNonNull(
        outputFilesAde.stream().filter(file -> file.getName().contains("02.csv")).map(File::toPath)
            .findFirst().orElse(null)));
    assertThat(outputSecondChunkAdeContent).hasSize(2);

    List<String> outputFileAdeContent = Stream.concat(outputFirstChunkAdeContent.stream(),
            outputSecondChunkAdeContent.stream())
        .collect(Collectors.toList());

    // Check that output files contain expected lines
    Set<String> expectedOutputFileTrnContent = getExpectedTrnOutputFileContent();
    Set<String> expectedOutputFileAdeContent = getExpectedAdeOutputFileContent();

    Assert.assertEquals(expectedOutputFileTrnContent, new HashSet<>(outputFileTrnContent));
    Assert.assertEquals(
        "#sha256sum:8bca0fdabf06e1c30b716224c67a5753ac5d999cf6a375ac7adba16f725f2046",
        outputFileTrnContent.get(0));
    Assert.assertEquals(expectedOutputFileAdeContent, new HashSet<>(outputFileAdeContent));
    Assert.assertEquals(
        "#sha256sum:8bca0fdabf06e1c30b716224c67a5753ac5d999cf6a375ac7adba16f725f2046",
        outputFileAdeContent.get(0));

    // Check that encrypted output files have the same content of unencrypted ones
    Collection<File> adeEncryptedFiles = outputPgpFiles.stream()
        .filter(p -> p.getName().startsWith("ADE.99999.TRNLOG.20220204.094652.001")
            && p.getName().endsWith(".pgp")).collect(Collectors.toList());
    Set<String> encryptedFilesContent = new HashSet<>();
    for (File adeEncryptedFile : adeEncryptedFiles) {

      FileInputStream trxEncFileIS = new FileInputStream(adeEncryptedFile);
      FileInputStream secretFilePathIS = null;
      try {
        String secretKeyPath =
            "file:/" + this.getClass().getResource("/test-encrypt").getFile() + "/secretKey.asc";
        Resource secretKeyResource = resolver.getResource(secretKeyPath);

        secretFilePathIS = new FileInputStream(secretKeyResource.getFile());
        byte[] trxEncFileDecryptedFileData = EncryptUtil.decryptFile(trxEncFileIS, secretFilePathIS,
            "test".toCharArray());
        File trxEncFileDecryptedFile = tempFolder.newFile(
            "trxEncFileDecrypted_" + getChunkNumberFromFilename(adeEncryptedFile.getName())
                + ".csv");
        FileUtils.writeByteArrayToFile(trxEncFileDecryptedFile, trxEncFileDecryptedFileData);

        List<String> trxEncFileDecryptedFileContent = Files.readAllLines(
            trxEncFileDecryptedFile.toPath().toAbsolutePath());
        encryptedFilesContent.addAll(trxEncFileDecryptedFileContent);
      } finally {
        trxEncFileIS.close();
        secretFilePathIS.close();
      }
    }

    Assert.assertEquals(expectedOutputFileAdeContent, encryptedFilesContent);
  }

  private String getChunkNumberFromFilename(String filename) {
    return filename.split("\\.")[6];
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
        Objects.requireNonNull(this.getClass().getResource("/test-encrypt/pan")).getFile()
            + "/pan.csv",
        EncryptUtil.readPublicKey(
            this.getClass().getResourceAsStream("/test-encrypt/publicKey.asc")),
        false, false);

    panPgpFOS.close();
  }

  @SneakyThrows
  private Set<File> createAdeOutputFiles() {
    Set<File> files = new HashSet<>();
    files.add(new File(resolver.getResource("classpath:/test-encrypt/output")
        .getFile().getAbsolutePath() + "/ADE.99999.TRNLOG.20220204.094652.001.01.csv"));
    files.add(new File(resolver.getResource("classpath:/test-encrypt/output")
        .getFile().getAbsolutePath() + "/ADE.99999.TRNLOG.20220204.094652.001.02.csv"));

    for (File file : files) {
      file.createNewFile();
    }

    return files;
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
        resolver.getResources("classpath:/test-encrypt/output")[0].getFile(), new String[]{"pgp"},
        false);
  }

  private Set<String> getExpectedPgpFilenames() {
    Set<String> expectedPgpFilenames = new HashSet<>();
    expectedPgpFilenames.add("CSTAR.99999.TRNLOG.20220204.094652.001.csv.pgp");
    expectedPgpFilenames.add("ADE.99999.TRNLOG.20220204.094652.001.01.csv.pgp");
    expectedPgpFilenames.add("ADE.99999.TRNLOG.20220204.094652.001.02.csv.pgp");
    return expectedPgpFilenames;
  }

  @SneakyThrows
  private Collection<File> getOutputCsvFiles() {
    return FileUtils.listFiles(
        resolver.getResources("classpath:/test-encrypt/output")[0].getFile(), new String[]{"csv"},
        false);
  }

  private Set<String> getExpectedCsvFileNames() {
    Set<String> expectedCsvFilenames = new HashSet<>();
    expectedCsvFilenames.add("CSTAR.99999.TRNLOG.20220204.094652.001.csv");
    expectedCsvFilenames.add("ADE.99999.TRNLOG.20220204.094652.001.01.csv");
    expectedCsvFilenames.add("ADE.99999.TRNLOG.20220204.094652.001.02.csv");
    return expectedCsvFilenames;
  }

  private Set<String> getExpectedTrnOutputFileContent() {
    Set<String> expectedOutputFileTrnContent = new HashSet<>();
    expectedOutputFileTrnContent.add(
        "#sha256sum:8bca0fdabf06e1c30b716224c67a5753ac5d999cf6a375ac7adba16f725f2046");
    expectedOutputFileTrnContent.add(
        "99999;00;00;28aa47c8c6cd1a6b0a86ebe18471295796c88269868825b4cd41f94f0a07e88e;03/20/2020 10:50:33;1111111111;5555;;1111;978;22222;0000;1;000002;5422;fis123;12345678901;00;");
    expectedOutputFileTrnContent.add(
        "99999;00;01;e2df0a82ac0aa12921c398e1eba9119772db868650ebef22b8919fa0fb7642ed;03/20/2020 11:23:00;333333333;7777;;3333;978;4444;0000;1;000002;5422;fis123;12345678901;00;");
    expectedOutputFileTrnContent.add(
        "99999;01;00;805f89015f85948f7d7bdd57a0a81e4cd95fc81bdd1195a69c4ab139f0ebed7b;03/20/2020 11:04:53;2222222222;6666;;2222;978;3333;0000;1;000002;5422;fis123;12345678901;00;par2");
    return expectedOutputFileTrnContent;
  }

  private Set<String> getExpectedAdeOutputFileContent() {
    String transmissionDate = getDateFormattedAsString();

    Set<String> expectedOutputFileAdeContent = new HashSet<>();
    expectedOutputFileAdeContent.add(
        "#sha256sum:8bca0fdabf06e1c30b716224c67a5753ac5d999cf6a375ac7adba16f725f2046");
    expectedOutputFileAdeContent.add("99999;00;" + transmissionDate
        + ";03/20/2020;2;6666;978;4444;0000;1;fis123;12345678901;00");
    expectedOutputFileAdeContent.add("99999;01;" + transmissionDate
        + ";03/20/2020;1;2222;978;3333;0000;1;fis123;12345678901;00");
    expectedOutputFileAdeContent.add("99999;00;" + transmissionDate
        + ";03/20/2020;1;1111;978;22222;0000;1;fis123;12345678901;00");

    return expectedOutputFileAdeContent;
  }

  private String getDateFormattedAsString() {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return OffsetDateTime.now().format(fmt);
  }
}