package it.gov.pagopa.rtd.transaction_filter.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.connector.TransactionSftpConnector;
import lombok.SneakyThrows;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.io.File;

public class SftpConnectorServiceTest {

    public SftpConnectorServiceTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    TransactionSftpConnector transactionSftpConnectorMock;

    SftpConnectorService sftpConnectorService;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/").getFile()));

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        Mockito.reset(transactionSftpConnectorMock);
        this.sftpConnectorService = new SftpConnectorServiceImpl(transactionSftpConnectorMock);
    }

    @SneakyThrows
    @Test
    public void sendFile_OK() {
        File file = tempFolder.newFile("testFile");
        sftpConnectorService.transferFile(file);
        BDDMockito.verify(transactionSftpConnectorMock).sendFile(Mockito.eq(file));
    }

    @SneakyThrows
    @Test
    public void sendFile_KO() {
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(transactionSftpConnectorMock).sendFile(Mockito.eq(null));
        expectedException.expect(Exception.class);
        sftpConnectorService.transferFile(null);
        BDDMockito.verify(transactionSftpConnectorMock).sendFile(Mockito.eq(null));
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}