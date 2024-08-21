package it.gov.pagopa.rtd.transaction_filter.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import it.gov.pagopa.rtd.transaction_filter.batch.step.PanReaderStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@ExtendWith(MockitoExtension.class)
class BatchExecutorTest {

  public static final String FILENAME_DUMMY = "CSTAR.12345.TRNLOG.20231013.123407.001.csv";
  @Mock
  JobLauncher jobLauncher;
  @Mock
  TransactionFilterStep transactionFilterStep;
  @Mock
  PathMatchingResourcePatternResolver mockResolver;
  @Mock
  PanReaderStep panReaderStep;
  @Mock
  StoreService storeService;
  BatchExecutor batchExecutor;

  AutoCloseable closeable;
  PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

  @TempDir
  private Path tempInputFolder;
  @TempDir
  private Path linkTestFolder;

  static class JobExecutionPreconditionsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
      return Stream.of(
          Arguments.of(new Resource[]{new FileSystemResource("file.csv")},
              false, new Resource[]{new FileSystemResource("hpan.csv")}, 0),
          Arguments.of(new Resource[]{}, false, new Resource[]{}, 0),
          Arguments.of(new Resource[]{new FileSystemResource("CSTAR.12345.TRNLOG.20230230.161746.001.csv")},
              false, new Resource[]{new FileSystemResource("hpan.csv")}, 1),
          Arguments.of(new Resource[]{new FileSystemResource("CSTAR.12345.TRNLOG.20230230.161746.001.csv")},
              true, new Resource[]{new FileSystemResource("hpan.csv")}, 1),
          Arguments.of(new Resource[]{new FileSystemResource("CSTAR.12345.TRNLOG.20230230.161746.001.csv")},
              true, new Resource[]{}, 1),
          Arguments.of(new Resource[]{new FileSystemResource("CSTAR.12345.TRNLOG.20230230.161746.001.csv")},
              false, new Resource[]{}, 0),
          Arguments.of(new Resource[]{new FileSystemResource("CSTAR.12345.TRNLOG.20230230.161746.001.csv"),
                  new FileSystemResource("CSTAR.12345.TRNLOG.20230301.161746.001.csv")}, true, new Resource[]{}, 1)
      );
    }
  }

  @BeforeEach
  void setUp() {
    batchExecutor = new BatchExecutor(null, jobLauncher, transactionFilterStep, panReaderStep, storeService, resolver);
  }

  @SneakyThrows
  @ParameterizedTest
  @ArgumentsSource(JobExecutionPreconditionsProvider.class)
  void batchExecutorWithParameters(Resource[] transactionFilesMocked,
      boolean hpanRecoveryEnabled,
      Resource[] hpanFilesMocked,
      int nTimesJobRun) {
    var batchExecutorWithMockedResolver = new BatchExecutor(null, jobLauncher, transactionFilterStep, panReaderStep, storeService, mockResolver);
    String resourcesPath = "/input";
    String hpanPath = "/hpan";
    BDDMockito.when(transactionFilterStep.getTransactionDirectoryPath())
        .thenReturn(resourcesPath);
    BDDMockito.when(mockResolver.getResources(resourcesPath + "/*.csv")).thenReturn(transactionFilesMocked);
    BDDMockito.when(panReaderStep.getHpanDirectoryPath()).thenReturn(hpanPath);
    BDDMockito.when(mockResolver.getResources(hpanPath)).thenReturn(hpanFilesMocked);
    batchExecutorWithMockedResolver.setHpanListRecoveryEnabled(hpanRecoveryEnabled);

    batchExecutorWithMockedResolver.execute(new Date());

    Mockito.verify(jobLauncher, Mockito.times(nTimesJobRun)).run(any(), any());
  }

  @SneakyThrows
  @Test
  void givenSymlinkWhenReadFilesThenFoundExpectedFiles() {
    // create a file inside a temp folder and create a symlink to that folder
    var folderTarget = Path.of(linkTestFolder.toString(), "targetFolder/");
    Path inputFile = Files.createFile(tempInputFolder.resolve(FILENAME_DUMMY));
    Files.createSymbolicLink(folderTarget, inputFile.getParent());

    var resources = batchExecutor.getCsvResources("file:" + folderTarget);

    assertThat(resources).isNotEmpty().hasSize(1);
    assertThat(resources[0].getFile()).hasName(FILENAME_DUMMY);
  }

  @SneakyThrows
  @Test
  void givenRealDirectoryWhenReadFilesThenFoundExpectedFiles() {
    Files.createFile(tempInputFolder.resolve(FILENAME_DUMMY));

    var resources = batchExecutor.getCsvResources("file:" + tempInputFolder);

    assertThat(resources).isNotEmpty().hasSize(1);
    assertThat(resources[0].getFile()).hasName(FILENAME_DUMMY);
  }

  @SneakyThrows
  @Test
  void givenFilenameWithoutFilePrefixWhenResolvePathThenFoundExpectedFiles() {
    Files.createFile(tempInputFolder.resolve(FILENAME_DUMMY));

    var resources = batchExecutor.getCsvResources(tempInputFolder.toString());

    assertThat(resources).isNotEmpty().hasSize(1);
    assertThat(resources[0].getFile()).hasName(FILENAME_DUMMY);
  }
}