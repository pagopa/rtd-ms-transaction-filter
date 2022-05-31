package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.mockito.Mockito.verify;

import it.gov.pagopa.rtd.transaction_filter.connector.AbiToFiscalCodeRestClient;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;

class AbiToFiscalCodeMapRecoveryTaskletTest {

  private ChunkContext chunkContext;
  private StepExecution execution;
  private AbiToFiscalCodeMapRecoveryTasklet tasklet;
  private Map<String, String> abiToFiscalCodeDefaultMap;

  @Mock
  private StoreService storeServiceMock;
  @Mock
  private AbiToFiscalCodeRestClient restClient;

  @BeforeEach
  void setup() {
    MockitoAnnotations.initMocks(this);

    execution = MetaDataInstanceFactory.createStepExecution();
    StepContext stepContext = new StepContext(execution);
    chunkContext = new ChunkContext(stepContext);

    tasklet = createDefaultTasklet();
    abiToFiscalCodeDefaultMap = createDefaultMap();
  }

  @Test
  void setAbiToFiscalCodeMapCorrectly() {
    BDDMockito.doReturn(abiToFiscalCodeDefaultMap).when(restClient)
        .getFakeAbiToFiscalCodeMap();

    tasklet.execute(new StepContribution(execution), chunkContext);

    verify(restClient, Mockito.times(1)).getFakeAbiToFiscalCodeMap();
    verify(storeServiceMock, Mockito.times(1)).setAbiToFiscalCodeMap(abiToFiscalCodeDefaultMap);
  }

  @Test
  void whenAbiToFiscalCodeMapIsNullThenDoNotSaveIt() {
    BDDMockito.doReturn(null).when(restClient).getFakeAbiToFiscalCodeMap();

    tasklet.execute(new StepContribution(execution), chunkContext);

    verify(restClient, Mockito.times(1)).getFakeAbiToFiscalCodeMap();
    verify(storeServiceMock, Mockito.times(0)).setAbiToFiscalCodeMap(new HashMap<>());
  }

  @Test
  void whenTaskletIsNotEnabledThenDoNothing() {
    tasklet.setTaskletEnabled(false);
    BDDMockito.doReturn(abiToFiscalCodeDefaultMap).when(restClient)
        .getFakeAbiToFiscalCodeMap();

    tasklet.execute(new StepContribution(execution), chunkContext);

    verify(restClient, Mockito.times(0)).getFakeAbiToFiscalCodeMap();
    verify(storeServiceMock, Mockito.times(0)).setAbiToFiscalCodeMap(abiToFiscalCodeDefaultMap);
  }

  AbiToFiscalCodeMapRecoveryTasklet createDefaultTasklet() {
    AbiToFiscalCodeMapRecoveryTasklet tasklet = new AbiToFiscalCodeMapRecoveryTasklet(
        restClient, storeServiceMock);
    tasklet.setTaskletEnabled(true);
    return tasklet;
  }

  Map<String, String> createDefaultMap() {
    Map<String, String> abiToFiscalCodeMap = new HashMap<>();
    abiToFiscalCodeMap.put("STPAY", "123456789");
    return abiToFiscalCodeMap;
  }
}