package it.gov.pagopa.rtd.transaction_filter.batch.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class PathResolverTest {

  public static final String FILENAME_DUMMY = "CSTAR.12345.TRNLOG.20231013.123407.001.csv";

  private final PathResolver pathResolver = new PathResolver(new PathMatchingResourcePatternResolver());

  @TempDir
  private Path tempInputFolder;
  @TempDir
  private Path linkTestFolder;

  @SneakyThrows
  @Test
  void givenSymlinkWhenReadFilesThenFoundExpectedFiles() {
    // create a file inside a temp folder and create a symlink to that folder
    var folderTarget = Path.of(linkTestFolder.toString(), "targetFolder/");
    Path inputFile = Files.createFile(tempInputFolder.resolve(FILENAME_DUMMY));
    Files.createSymbolicLink(folderTarget, inputFile.getParent());

    var resources = pathResolver.getCsvResources("file:" + folderTarget);

    assertThat(resources).isNotEmpty().hasSize(1);
    assertThat(resources[0].getFile()).hasName(FILENAME_DUMMY);
  }

  @SneakyThrows
  @Test
  void givenRealDirectoryWhenReadFilesThenFoundExpectedFiles() {
    Files.createFile(tempInputFolder.resolve(FILENAME_DUMMY));

    var resources = pathResolver.getCsvResources("file:" + tempInputFolder);

    assertThat(resources).isNotEmpty().hasSize(1);
    assertThat(resources[0].getFile()).hasName(FILENAME_DUMMY);
  }

  @SneakyThrows
  @Test
  void givenFilenameWithoutFilePrefixWhenResolvePathThenRaiseException() {
    Files.createFile(tempInputFolder.resolve(FILENAME_DUMMY));

    assertThatThrownBy(() -> pathResolver.getCsvResources(tempInputFolder.toString()))
        .isInstanceOf(FileNotFoundException.class);
  }

  @SneakyThrows
  @Test
  void givenEmptyDirectoryWhenResolvePathThenReturnEmptyArray() {
    var resources = pathResolver.getCsvResources("file:" + tempInputFolder);

    assertThat(resources).isEmpty();
  }

  @SneakyThrows
  @Test
  void givenDirectoryWithFileNotCsvWhenResolvePathThenReturnEmptyArray() {
    Files.createFile(tempInputFolder.resolve("not-a-csv-file.txt"));

    var resources = pathResolver.getCsvResources("file:" + tempInputFolder);

    assertThat(resources).isEmpty();
  }

  @SneakyThrows
  @Test
  void givenClassPathWhenResolvePathThenReturnFiles() {
    var resources = pathResolver.getCsvResources("classpath:test-encrypt/transactions");

    assertThat(resources).isNotEmpty().hasSize(1);
  }

  @SneakyThrows
  @Test
  void givenClassPathWhenResolvePathThenReturnEmptyArray() {
    var resources = pathResolver.getCsvResources("classpath:test-encrypt");

    assertThat(resources).isEmpty();
  }

}