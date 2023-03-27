package it.gov.pagopa.rtd.transaction_filter.utils;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HpanUnzipperTest {

  @TempDir
  Path tempDir;


  @Test
  void givenThresholdSizeWhenExtractZipFileThenThrowException() {

    File zippedFile = createTempZipFile(1, 10);

    HpanUnzipper hpanUnzipper = HpanUnzipper.builder()
        .fileToUnzip(zippedFile)
        .zipThresholdEntries(1000)
        .thresholdSizeUncompressed(5)
        .outputDirectory(tempDir)
        .isFilenameValidPredicate(file -> true)
        .build();

    assertThatThrownBy(hpanUnzipper::extractZipFile).isInstanceOf(
        IOException.class);
  }

  @Test
  void givenThresholdEntriesWhenExtractZipFileThenThrowException() {

    File zippedFile = createTempZipFile(10, 1);

    HpanUnzipper hpanUnzipper = HpanUnzipper.builder()
        .fileToUnzip(zippedFile)
        .zipThresholdEntries(5)
        .thresholdSizeUncompressed(500000)
        .outputDirectory(tempDir)
        .isFilenameValidPredicate(file -> true)
        .listFilePattern("\\.csv")
        .build();

    assertThatThrownBy(hpanUnzipper::extractZipFile).isInstanceOf(
        IOException.class);
  }

  @Test
  void givenWrongPatternWhenExtractZipFileThenReturnsNoFile() {

    File zippedFile = createTempZipFile(1, 10);

    HpanUnzipper hpanUnzipper = HpanUnzipper.builder()
        .fileToUnzip(zippedFile)
        .zipThresholdEntries(50)
        .thresholdSizeUncompressed(500000)
        .outputDirectory(tempDir)
        .isFilenameValidPredicate(file -> true)
        .listFilePattern("\\.csv")
        .build();

    assertThat(hpanUnzipper.extractZipFile()).isNull();
  }

  @Test
  void givenHappyCaseWhenExtractZipFileThenReturnsFile() {

    File zippedFile = createTempZipFile(1, 10);

    HpanUnzipper hpanUnzipper = HpanUnzipper.builder()
        .fileToUnzip(zippedFile)
        .zipThresholdEntries(50)
        .thresholdSizeUncompressed(500000)
        .outputDirectory(tempDir)
        .isFilenameValidPredicate(file -> file.matches(".*\\.txt"))
        .listFilePattern(".*\\.txt")
        .build();

    assertThat(hpanUnzipper.extractZipFile()).isNotNull();
  }

  @Test
  void givenBadPredicateWhenExtractZipFileThenReturnsFile() {

    File zippedFile = createTempZipFile(1, 10);

    HpanUnzipper hpanUnzipper = HpanUnzipper.builder()
        .fileToUnzip(zippedFile)
        .zipThresholdEntries(50)
        .thresholdSizeUncompressed(500000)
        .outputDirectory(tempDir)
        .isFilenameValidPredicate(file -> file.matches(".*\\.csv"))
        .listFilePattern(".*\\.txt")
        .build();

    assertThatThrownBy(hpanUnzipper::extractZipFile).isInstanceOf(
        IOException.class);
  }

  @SneakyThrows
  private File createTempZipFile(int files, int rowsPerFile) {

    File zippedFile = tempDir.resolve("file.zip").toFile();
    try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zippedFile.toPath()))) {

      for (int i = 0; i < files; i++) {
        ZipEntry e = new ZipEntry("file" + i + ".txt");
        out.putNextEntry(e);

        for (int j = 0; j < rowsPerFile; j++) {
          String stringHashed = DigestUtils.sha256Hex(
              String.valueOf(SecureRandom.getInstanceStrong().nextInt()));
          byte[] data = stringHashed.getBytes();
          out.write(data, 0, data.length);
        }
        out.closeEntry();
      }
    }
    return zippedFile;
  }

}