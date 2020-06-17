package it.gov.pagopa.rtd.transaction_filter.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
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

public class HpanConnectorServiceTest {

    public HpanConnectorServiceTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    private HpanRestClient hpanRestClientMock;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        Mockito.reset(hpanRestClientMock);
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/").getFile()));

    @Test
    public void testSalt_OK() {
        BDDMockito.doReturn("testSalt").when(hpanRestClientMock).getSalt();
        String salt = hpanRestClientMock.getSalt();
        Assert.assertEquals("testSalt", salt);
        BDDMockito.verify(hpanRestClientMock).getSalt();
    }

    @Test
    public void testSalt_KO() {
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(hpanRestClientMock).getSalt();
        expectedException.expect(Exception.class);
        hpanRestClientMock.getSalt();
        BDDMockito.verify(hpanRestClientMock).getSalt();
    }

    @SneakyThrows
    @Test
    public void testList_OK() {
        File file = tempFolder.newFile("testFile");
        BDDMockito.doReturn(file).when(hpanRestClientMock).getList();
        File returnedFile = hpanRestClientMock.getList();
        Assert.assertEquals(file, returnedFile);
        BDDMockito.verify(hpanRestClientMock).getList();
    }

    @SneakyThrows
    @Test
    public void testList_KO() {
        File file = tempFolder.newFile("testFile");
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(hpanRestClientMock).getList();
        expectedException.expect(Exception.class);
        hpanRestClientMock.getList();
        BDDMockito.verify(hpanRestClientMock).getList();
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}