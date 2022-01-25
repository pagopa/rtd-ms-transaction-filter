package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SasResponse;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
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
import org.springframework.core.io.UrlResource;

import java.io.File;
import java.util.Objects;

public class TransactionSenderRestTaskletTest {

    public TransactionSenderRestTaskletTest() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Mock
    private HpanConnectorService hpanConnectorServiceMock;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(Objects.requireNonNull(getClass().getResource("/test-encrypt")).getFile()));

    @SneakyThrows
    @Before
    public void setUp() {
        Mockito.reset(hpanConnectorServiceMock);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @SneakyThrows
    @Test
    public void testExecuteTaskletWhenEnabled() {
        SasResponse sasResponseMock = new SasResponse();
        sasResponseMock.setSas("sas-token");
        sasResponseMock.setAuthorizedContainer("authorized-container");
        BDDMockito.doReturn(sasResponseMock).when(hpanConnectorServiceMock).getSasToken(HpanRestClient.SasScope.ADE);

        File fileToSend = tempFolder.newFile("test");

        TransactionSenderRestTasklet transactionSenderRestTasklet = new TransactionSenderRestTasklet();
        transactionSenderRestTasklet.setTaskletEnabled(true);
        transactionSenderRestTasklet.setHpanConnectorService(this.hpanConnectorServiceMock);
        transactionSenderRestTasklet.setResource(new UrlResource("file:/" + fileToSend.getPath()));
        transactionSenderRestTasklet.setScope(HpanRestClient.SasScope.ADE);

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        transactionSenderRestTasklet.execute(new StepContribution(execution), chunkContext);

        BDDMockito.verify(hpanConnectorServiceMock).getSasToken(HpanRestClient.SasScope.ADE);
        BDDMockito.verify(hpanConnectorServiceMock).uploadFile(fileToSend, sasResponseMock.getSas(), sasResponseMock.getAuthorizedContainer());
    }

    @SneakyThrows
    @Test
    public void testExecuteTaskletWhenDisabled() {
        File fileToSend = tempFolder.newFile("test");

        TransactionSenderRestTasklet transactionSenderRestTasklet = new TransactionSenderRestTasklet();
        transactionSenderRestTasklet.setTaskletEnabled(false);
        transactionSenderRestTasklet.setHpanConnectorService(this.hpanConnectorServiceMock);
        transactionSenderRestTasklet.setResource(new UrlResource("file:/" + fileToSend.getPath()));
        transactionSenderRestTasklet.setScope(HpanRestClient.SasScope.ADE);

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        transactionSenderRestTasklet.execute(new StepContribution(execution), chunkContext);

        BDDMockito.verify(hpanConnectorServiceMock, Mockito.times(0)).getSasToken(Mockito.any());
        BDDMockito.verify(hpanConnectorServiceMock, Mockito.times(0)).uploadFile(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}