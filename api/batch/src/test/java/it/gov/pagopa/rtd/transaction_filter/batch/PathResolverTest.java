package it.gov.pagopa.rtd.transaction_filter.batch;

import static org.assertj.core.api.Assertions.assertThat;

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
  void givenFilenameWithoutFilePrefixWhenResolvePathThenFoundExpectedFiles() {
    Files.createFile(tempInputFolder.resolve(FILENAME_DUMMY));

    var resources = pathResolver.getCsvResources(tempInputFolder.toString());

    assertThat(resources).isNotEmpty().hasSize(1);
    assertThat(resources[0].getFile()).hasName(FILENAME_DUMMY);
  }

}