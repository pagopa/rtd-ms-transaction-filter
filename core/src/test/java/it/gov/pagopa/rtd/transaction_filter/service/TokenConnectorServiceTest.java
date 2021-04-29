package it.gov.pagopa.rtd.transaction_filter.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.connector.TokenPanRestClient;
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
import java.util.Collections;
import java.util.List;

public class TokenConnectorServiceTest {

    public TokenConnectorServiceTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    private TokenPanRestClient tokenPanRestClientMock;
    private TokenConnectorService tokenConnectorService;


    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        Mockito.reset(tokenPanRestClientMock);
        tokenConnectorService = new TokenConnectorServiceImpl(tokenPanRestClientMock);
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/").getFile()));

    @SneakyThrows
    @Test
    public void testTokenList_OK() {
        File file = tempFolder.newFile("testFile");
        BDDMockito.doReturn(Collections.singletonList(file)).when(tokenPanRestClientMock).getTokenList();
        List<File> returnedFile = tokenConnectorService.getTokenPanList();
        Assert.assertEquals(file, returnedFile.get(0));
        BDDMockito.verify(tokenPanRestClientMock).getTokenList();
    }

    @SneakyThrows
    @Test
    public void testList_KO() {
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(tokenPanRestClientMock).getTokenList();
        expectedException.expect(Exception.class);
        tokenConnectorService.getTokenPanList();
        BDDMockito.verify(tokenPanRestClientMock).getTokenList();
    }

    @SneakyThrows
    @Test
    public void testTokenBin_OK() {
        File file = tempFolder.newFile("testFile");
        BDDMockito.doReturn(Collections.singletonList(file)).when(tokenPanRestClientMock).getBinList();
        List<File> returnedFile = tokenConnectorService.getBinList();
        Assert.assertEquals(file, returnedFile.get(0));
        BDDMockito.verify(tokenPanRestClientMock).getBinList();
    }

    @SneakyThrows
    @Test
    public void testBin_KO() {
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(tokenPanRestClientMock).getBinList();
        expectedException.expect(Exception.class);
        tokenConnectorService.getBinList();
        BDDMockito.verify(tokenPanRestClientMock).getBinList();
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}