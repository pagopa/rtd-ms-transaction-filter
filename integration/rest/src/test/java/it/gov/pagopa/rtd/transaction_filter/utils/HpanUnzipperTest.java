package it.gov.pagopa.rtd.transaction_filter.utils;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HpanUnzipperTest {

  @TempDir
  Path tempDir;

  Set<String> dataInFile;

  @BeforeEach
  void setUp() {
    dataInFile = new HashSet<>();
  }

  @Test
  void givenThresholdSizeWhenExtractZipFileThenThrowException() {

    File zippedFile = createTempZipFile(1, 10);

    HpanUnzipper hpanUnzipper = createDefaultUnzipper(zippedFile);
    hpanUnzipper.setZipThresholdEntries(1000);
    hpanUnzipper.setThresholdSizeUncompressed(5);

    assertThatThrownBy(hpanUnzipper::extractZipFile).isInstanceOf(
        IOException.class);
  }

  @Test
  void givenThresholdEntriesWhenExtractZipFileThenThrowException() {

    File zippedFile = createTempZipFile(10, 1);

    HpanUnzipper hpanUnzipper = createDefaultUnzipper(zippedFile);
    hpanUnzipper.setZipThresholdEntries(5);

    assertThatThrownBy(hpanUnzipper::extractZipFile).isInstanceOf(
        IOException.class);
  }

  @Test
  void givenWrongPatternWhenExtractZipFileThenReturnsNoFile() {

    File zippedFile = createTempZipFile(1, 10);

    HpanUnzipper hpanUnzipper = createDefaultUnzipper(zippedFile);
    hpanUnzipper.setListFilePattern("\\.txt");

    assertThat(hpanUnzipper.extractZipFile()).isNull();
  }

  @Test
  @SneakyThrows
  void givenHappyCaseWhenExtractZipFileThenReturnsFile() {

    File zippedFile = createTempZipFile(1, 10);

    HpanUnzipper hpanUnzipper = createDefaultUnzipper(zippedFile);

    File extractedFile = hpanUnzipper.extractZipFile();

    assertThat(extractedFile).isNotNull();
    Collection<String> readLines = Files.readAllLines(extractedFile.toPath());
    assertThat(readLines).containsAll(dataInFile);
  }

  @Test
  void givenBadPredicateWhenExtractZipFileThenThrowException() {

    File zippedFile = createTempZipFile(1, 10);

    HpanUnzipper hpanUnzipper = createDefaultUnzipper(zippedFile);
    hpanUnzipper.setIsFilenameValidPredicate(file -> file.matches(".*\\.txt"));

    assertThatThrownBy(hpanUnzipper::extractZipFile).isInstanceOf(
        IOException.class);
  }

  @Test
  void givenZipSlipAttackWhenExtractZipFileThenThrowException() {

    File zippedFile = createTempZipFile(1, 10, true);

    HpanUnzipper hpanUnzipper = createDefaultUnzipper(zippedFile);

    assertThatThrownBy(hpanUnzipper::extractZipFile).isInstanceOf(
        ZipException.class);
  }

  @Test
  void givenLowCompressionRatioWhenExtractZipFileThenThrowException() {

    File zippedFile = createTempZipFile(1, 10);

    HpanUnzipper hpanUnzipper = createDefaultUnzipper(zippedFile);
    hpanUnzipper.setThresholdRatio(0.5);

    assertThatThrownBy(hpanUnzipper::extractZipFile).isInstanceOf(
        IOException.class);
  }

  private HpanUnzipper createDefaultUnzipper(File zippedFile) {
    return HpanUnzipper.builder()
        .fileToUnzip(zippedFile)
        .zipThresholdEntries(50)
        .thresholdSizeUncompressed(500000)
        .outputDirectory(tempDir)
        .isFilenameValidPredicate(file -> file.matches(".*\\.csv"))
        .listFilePattern(".*\\.csv")
        .thresholdRatio(10)
        .build();
  }

  private File createTempZipFile(int files, int rowsPerFile) {
    return createTempZipFile(files, rowsPerFile, false);
  }

  @SneakyThrows
  private File createTempZipFile(int files, int rowsPerFile, boolean zipSplitAttack) {

    File zippedFile = tempDir.resolve("file.zip").toFile();
    try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zippedFile.toPath()))) {

      for (int i = 0; i < files; i++) {
        String zipEntryName = zipSplitAttack ? "../../file" + i + ".csv" : "file" + i + ".csv";
        ZipEntry e = new ZipEntry(zipEntryName);
        out.putNextEntry(e);

        for (int j = 0; j < rowsPerFile; j++) {
          String stringHashed = DigestUtils.sha256Hex(
              String.valueOf(SecureRandom.getInstanceStrong().nextInt()));
          byte[] data = (stringHashed + "\n").getBytes();
          out.write(data, 0, data.length);
          dataInFile.add(stringHashed);
        }
        out.closeEntry();
      }
    }
    return zippedFile;
  }

}