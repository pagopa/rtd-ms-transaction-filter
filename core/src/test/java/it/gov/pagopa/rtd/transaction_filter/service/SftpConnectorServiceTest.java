package it.gov.pagopa.rtd.transaction_filter.service;

import eu.sia.meda.BaseTest;
import it.gov.pagopa.rtd.transaction_filter.connector.TransactionSftpConnector;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.File;

public class SftpConnectorServiceTest extends BaseTest {

    @Mock
    TransactionSftpConnector transactionSftpConnectorMock;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/").getFile()));

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        Mockito.reset(transactionSftpConnectorMock);
    }

    @SneakyThrows
    @Test
    public void sendFile_OK() {
        File file = tempFolder.newFile("testFile");
        transactionSftpConnectorMock.sendFile(file);
        BDDMockito.verify(transactionSftpConnectorMock).sendFile(Mockito.eq(file));
    }

    @SneakyThrows
    @Test
    public void sendFile_KO() {
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(transactionSftpConnectorMock).sendFile(Mockito.eq(null));
        expectedException.expect(Exception.class);
        transactionSftpConnectorMock.sendFile(null);
        BDDMockito.verify(transactionSftpConnectorMock).sendFile(Mockito.eq(null));
    }


}