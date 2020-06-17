package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.SftpConnectorService;
import lombok.SneakyThrows;
import org.junit.*;
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

public class TransactionSenderTaskletTest {

    public TransactionSenderTaskletTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    private SftpConnectorService sftpConnectorServiceMock;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @SneakyThrows
    @Before
    public void setUp() {
        Mockito.reset(sftpConnectorServiceMock);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @SneakyThrows
    @Test
    public void testSendFile_OK() {
        File fileToSend = tempFolder.newFile("test");
        TransactionSenderTasklet transactionSenderTasklet = new TransactionSenderTasklet();
        transactionSenderTasklet.setSftpConnectorService(sftpConnectorServiceMock);
        transactionSenderTasklet.setResource(new UrlResource("file:/"+fileToSend.getPath()));
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        transactionSenderTasklet.setTaskletEnabled(true);
        transactionSenderTasklet.execute(new StepContribution(execution),chunkContext);
        BDDMockito.verify(sftpConnectorServiceMock).transferFile(Mockito.eq(fileToSend));
    }

    @SneakyThrows
    @Test
    public void testSendFile_KO() {
        File fileToSend = tempFolder.newFile("test");
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(sftpConnectorServiceMock).transferFile(Mockito.eq(fileToSend));
        TransactionSenderTasklet transactionSenderTasklet = new TransactionSenderTasklet();
        transactionSenderTasklet.setSftpConnectorService(sftpConnectorServiceMock);
        transactionSenderTasklet.setResource(new UrlResource("file:/"+fileToSend.getPath()));
        transactionSenderTasklet.setTaskletEnabled(true);
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        expectedException.expect(Exception.class);
        transactionSenderTasklet.execute(new StepContribution(execution),chunkContext);
        BDDMockito.verify(sftpConnectorServiceMock).transferFile(Mockito.eq(fileToSend));
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}