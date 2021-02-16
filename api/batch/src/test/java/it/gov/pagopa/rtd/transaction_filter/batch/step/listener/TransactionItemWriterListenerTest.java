package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class TransactionItemWriterListenerTest {

    public TransactionItemWriterListenerTest(){
        MockitoAnnotations.initMocks(this);
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @Mock
    private TransactionWriterService transactionWriterService;

    @Before
    public void initTest() {
        Mockito.reset(transactionWriterService);
        BDDMockito.doReturn(false)
                .when(transactionWriterService)
                .hasErrorHpan(Mockito.any());
        BDDMockito.doNothing()
                .when(transactionWriterService)
                .storeErrorPans(Mockito.any());
        BDDMockito.doNothing()
                .when(transactionWriterService)
                .write(Mockito.any(), Mockito.any());
    }

    @SneakyThrows
    @Test
    public void onWriteError_OK() {

        File folder = tempFolder.newFolder("testWriter");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemWriterListener transactionItemWriterListener = new TransactionItemWriterListener();
        transactionItemWriterListener.setExecutionDate(executionDate);
        transactionItemWriterListener.setEnableOnErrorLogging(true);
        transactionItemWriterListener.setEnableAfterWriteLogging(true);
        transactionItemWriterListener.setEnableOnErrorFileLogging(true);
        transactionItemWriterListener.setLoggingFrequency(1L);
        transactionItemWriterListener.setTransactionWriterService(transactionWriterService);
        transactionItemWriterListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemWriterListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemWriterListener.onWriteError(new Exception(), Collections.singletonList(InboundTransaction
                .builder().filename("test").lineNumber(1).build()));

        BDDMockito.verify(transactionWriterService).write(Mockito.any(),Mockito.any());


    }

    @SneakyThrows
    @Test
    public void onWriteError_OK_NoFileWritten() {

        File folder = tempFolder.newFolder("testWriter");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemWriterListener transactionItemWriterListener = new TransactionItemWriterListener();
        transactionItemWriterListener.setExecutionDate(executionDate);
        transactionItemWriterListener.setEnableOnErrorLogging(true);
        transactionItemWriterListener.setEnableAfterWriteLogging(true);
        transactionItemWriterListener.setEnableOnErrorFileLogging(false);
        transactionItemWriterListener.setLoggingFrequency(1L);
        transactionItemWriterListener.setTransactionWriterService(transactionWriterService);
        transactionItemWriterListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemWriterListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemWriterListener.onWriteError(new Exception(), Collections.singletonList(InboundTransaction
                .builder().filename("test").lineNumber(1).build()));

        BDDMockito.verify(transactionWriterService, Mockito.times(0)).write(Mockito.any(),Mockito.any());

    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}