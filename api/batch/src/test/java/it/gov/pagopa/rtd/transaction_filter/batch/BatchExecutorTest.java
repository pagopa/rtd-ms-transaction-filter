package it.gov.pagopa.rtd.transaction_filter.batch;

import static org.mockito.ArgumentMatchers.any;

import it.gov.pagopa.rtd.transaction_filter.batch.step.PanReaderStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep;
import it.gov.pagopa.rtd.transaction_filter.batch.utils.PathResolver;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.util.Date;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
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

@ExtendWith(MockitoExtension.class)
class BatchExecutorTest {

  @Mock
  JobLauncher jobLauncher;
  @Mock
  TransactionFilterStep transactionFilterStep;
  @Mock
  PanReaderStep panReaderStep;
  @Mock
  StoreService storeService;
  @Mock
  BatchExecutor batchExecutor;
  @Mock
  PathResolver pathResolver;

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
    batchExecutor = new BatchExecutor(null, jobLauncher, transactionFilterStep, panReaderStep, storeService, pathResolver);
  }

  @SneakyThrows
  @ParameterizedTest
  @ArgumentsSource(JobExecutionPreconditionsProvider.class)
  void batchExecutorWithParameters(Resource[] transactionFilesMocked,
      boolean hpanRecoveryEnabled,
      Resource[] hpanFilesMocked,
      int nTimesJobRun) {
    String resourcesPath = "/input";
    String hpanPath = "/hpan";
    BDDMockito.when(transactionFilterStep.getTransactionDirectoryPath())
        .thenReturn(resourcesPath);
    BDDMockito.when(pathResolver.getCsvResources(resourcesPath)).thenReturn(transactionFilesMocked);
    BDDMockito.when(panReaderStep.getHpanDirectoryPath()).thenReturn(hpanPath);
    BDDMockito.when(pathResolver.getResources(hpanPath)).thenReturn(hpanFilesMocked);
    batchExecutor.setHpanListRecoveryEnabled(hpanRecoveryEnabled);

    batchExecutor.execute(new Date());

    Mockito.verify(jobLauncher, Mockito.times(nTimesJobRun)).run(any(), any());
  }
}