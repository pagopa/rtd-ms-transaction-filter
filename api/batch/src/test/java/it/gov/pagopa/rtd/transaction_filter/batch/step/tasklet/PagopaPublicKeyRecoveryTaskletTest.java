package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;

public class PagopaPublicKeyRecoveryTaskletTest {

    public PagopaPublicKeyRecoveryTaskletTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Mock
    private HpanConnectorService hpanConnectorServiceMock;

    @Mock
    private StoreService storeServiceMock;

    @SneakyThrows
    @Before
    public void setUp() {
        Mockito.reset(hpanConnectorServiceMock, storeServiceMock);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @SneakyThrows
    @Test
    public void testExecuteTaskletWhenEnabled() {
        BDDMockito.doReturn("keyContent").when(hpanConnectorServiceMock).getPublicKey();
        BDDMockito.doNothing().when(storeServiceMock).storeKey("pagopa", "keyContent");
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        PagopaPublicKeyRecoveryTasklet pagopaPublicKeyRecoveryTasklet = new PagopaPublicKeyRecoveryTasklet();
        pagopaPublicKeyRecoveryTasklet.setHpanConnectorService(hpanConnectorServiceMock);
        pagopaPublicKeyRecoveryTasklet.setStoreService(storeServiceMock);
        pagopaPublicKeyRecoveryTasklet.setTaskletEnabled(true);
        pagopaPublicKeyRecoveryTasklet.execute(new StepContribution(execution),chunkContext);
        BDDMockito.verify(hpanConnectorServiceMock).getPublicKey();
        BDDMockito.verify(storeServiceMock).storeKey("pagopa", "keyContent");
    }

    @SneakyThrows
    @Test
    public void testExecuteTaskletWhenDisabled() {
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        PagopaPublicKeyRecoveryTasklet pagopaPublicKeyRecoveryTasklet = new PagopaPublicKeyRecoveryTasklet();
        pagopaPublicKeyRecoveryTasklet.setHpanConnectorService(hpanConnectorServiceMock);
        pagopaPublicKeyRecoveryTasklet.setStoreService(storeServiceMock);
        pagopaPublicKeyRecoveryTasklet.setTaskletEnabled(false);
        pagopaPublicKeyRecoveryTasklet.execute(new StepContribution(execution),chunkContext);
        BDDMockito.verify(hpanConnectorServiceMock, Mockito.times(0)).getPublicKey();
        BDDMockito.verify(storeServiceMock, Mockito.times(0)).storeKey("pagopa", "keyContent");
    }

}