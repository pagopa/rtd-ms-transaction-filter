package it.gov.pagopa.rtd.transaction_filter.service;

import eu.sia.meda.BaseTest;
import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;

public class HpanConnectorServiceTest extends BaseTest {

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


}