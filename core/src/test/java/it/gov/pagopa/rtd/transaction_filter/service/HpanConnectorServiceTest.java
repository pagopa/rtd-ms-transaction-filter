package it.gov.pagopa.rtd.transaction_filter.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SasResponse;
import lombok.SneakyThrows;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public class HpanConnectorServiceTest {

    public HpanConnectorServiceTest() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Mock
    private HpanRestClient hpanRestClientMock;
    private HpanConnectorService hpanConnectorService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        Mockito.reset(hpanRestClientMock);
        hpanConnectorService = new HpanConnectorServiceImpl(hpanRestClientMock);
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(Objects.requireNonNull(getClass().getResource("/")).getFile()));

    @Test
    public void testGetSaltReturnsSaltString() {
        BDDMockito.doReturn("testSalt").when(hpanRestClientMock).getSalt();
        String salt = hpanConnectorService.getSalt();
        Assert.assertEquals("testSalt", salt);
        BDDMockito.verify(hpanRestClientMock).getSalt();
    }

    @Test
    public void testGetSaltRaisesExceptionOnFailure() {
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(hpanRestClientMock).getSalt();
        expectedException.expect(Exception.class);
        hpanConnectorService.getSalt();
        BDDMockito.verify(hpanRestClientMock).getSalt();
    }

    @Test
    public void testGetPublicKeyReturnsKeyString() {
        BDDMockito.doReturn("keyValue").when(hpanRestClientMock).getPublicKey();
        String publicKey = hpanConnectorService.getPublicKey();
        Assert.assertEquals("keyValue", publicKey);
        BDDMockito.verify(hpanRestClientMock).getPublicKey();
    }

    @Test
    public void testGetPublicKeyRaisesExceptionOnFailure() {
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(hpanRestClientMock).getPublicKey();
        expectedException.expect(Exception.class);
        hpanConnectorService.getPublicKey();
        BDDMockito.verify(hpanRestClientMock).getPublicKey();
    }

    @SneakyThrows
    @Test
    public void testGetListReturnsHpanListFile() {
        File file = tempFolder.newFile("testFile");
        BDDMockito.doReturn(file).when(hpanRestClientMock).getList();
        File returnedFile = hpanConnectorService.getHpanList();
        Assert.assertEquals(file, returnedFile);
        BDDMockito.verify(hpanRestClientMock).getList();
    }

    @SneakyThrows
    @Test
    public void testGetListRaisesExceptionOnFailure() {
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(hpanRestClientMock).getList();
        expectedException.expect(Exception.class);
        hpanConnectorService.getHpanList();
        BDDMockito.verify(hpanRestClientMock).getList();
    }

    @Test
    public void testGetSasTokenOnAdeScopeReturnsSasResponse() {
        SasResponse fakeSasResponse = new SasResponse();
        fakeSasResponse.setSas("sig=1FKx%2F7lrOhV4YidvHmuW8rMP4lCG%2BqJ1pri%2FEpjXJtz%3D&st=2022-01-25T07:17Z&se=2022-01-25T08:17Z&spr=https&sp=rcw&sr=c&sv=2020-12-06");
        fakeSasResponse.setAuthorizedContainer("ade-transactions-tf6fecdd129fa27327d00bfbb11ece53e9c1d007123");
        BDDMockito.doReturn(fakeSasResponse).when(hpanRestClientMock).getSasToken(HpanRestClient.SasScope.ADE);
        SasResponse sas = hpanConnectorService.getSasToken(HpanRestClient.SasScope.ADE);
        Assert.assertEquals(sas, fakeSasResponse);
        BDDMockito.verify(hpanRestClientMock).getSasToken(HpanRestClient.SasScope.ADE);
    }

    @Test
    public void testGetSasTokenOnAdeScopeRaisesExceptionOnFailure() {
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(hpanRestClientMock).getSasToken(HpanRestClient.SasScope.ADE);
        expectedException.expect(Exception.class);
        hpanConnectorService.getSasToken(HpanRestClient.SasScope.ADE);
        BDDMockito.verify(hpanRestClientMock).getSasToken(HpanRestClient.SasScope.ADE);
    }

    @Test
    public void testGetSasTokenOnRtdScopeReturnsSasResponse() {
        SasResponse fakeSasResponse = new SasResponse();
        fakeSasResponse.setSas("sig=1FKx%2F7lrOhV4YidvHmuW8rMP4lCG%2BqJ1pri%2FEpjXJtz%3D&st=2022-01-25T07:17Z&se=2022-01-25T08:17Z&spr=https&sp=rcw&sr=c&sv=2020-12-06");
        fakeSasResponse.setAuthorizedContainer("rtd-transactions-tf6fecdd129fa27327d00bfbb11ece53e9c1d007123");
        BDDMockito.doReturn(fakeSasResponse).when(hpanRestClientMock).getSasToken(HpanRestClient.SasScope.RTD);
        SasResponse sas = hpanConnectorService.getSasToken(HpanRestClient.SasScope.RTD);
        Assert.assertEquals(sas, fakeSasResponse);
        BDDMockito.verify(hpanRestClientMock).getSasToken(HpanRestClient.SasScope.RTD);
    }

    @Test
    public void testGetSasTokenOnRtdScopeRaisesExceptionOnFailure() {
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(hpanRestClientMock).getSasToken(HpanRestClient.SasScope.RTD);
        expectedException.expect(Exception.class);
        hpanConnectorService.getSasToken(HpanRestClient.SasScope.RTD);
        BDDMockito.verify(hpanRestClientMock).getSasToken(HpanRestClient.SasScope.RTD);
    }

    @Test
    @SneakyThrows
    public void testUploadFile() {
        File fileToUpload = tempFolder.newFile("testFile");
        hpanConnectorService.uploadFile(fileToUpload, "sas-token", "authorized-container");
        BDDMockito.verify(hpanRestClientMock).uploadFile(fileToUpload, "sas-token", "authorized-container");
    }

    @Test
    public void testCleanAllTempFiles() {
        hpanConnectorService.cleanAllTempFiles();
        BDDMockito.verify(hpanRestClientMock).cleanTempFile();
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}