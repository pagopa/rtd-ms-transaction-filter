package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import static org.mockito.ArgumentMatchers.any;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.batch.utils.TransactionMaskPolicy;
import it.gov.pagopa.rtd.transaction_filter.batch.utils.TransactionMaskPolicyImpl;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.SneakyThrows;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionItemReaderListenerTest {

    private final TransactionMaskPolicy maskPolicy = new TransactionMaskPolicyImpl();

    public TransactionItemReaderListenerTest(){
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
                .hasErrorHpan(any());
        BDDMockito.doNothing()
                .when(transactionWriterService)
                .storeErrorPans(any());
        BDDMockito.doNothing()
                .when(transactionWriterService)
                .write(any(), any());
    }

    @SneakyThrows
    @Test
    public void afterRead_OK() {

        File folder = tempFolder.newFolder("testListener");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemReaderListener transactionItemReaderListener = new TransactionItemReaderListener(maskPolicy);
        transactionItemReaderListener.setExecutionDate(executionDate);
        transactionItemReaderListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemReaderListener.setEnableOnErrorLogging(true);
        transactionItemReaderListener.setEnableOnErrorFileLogging(true);
        transactionItemReaderListener.setEnableAfterReadLogging(true);
        transactionItemReaderListener.setLoggingFrequency(1L);
        transactionItemReaderListener.setTransactionWriterService(transactionWriterService);
        transactionItemReaderListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemReaderListener.afterRead(InboundTransaction
                .builder().filename("test").lineNumber(1).build());

        Assert.assertTrue(true);
    }

    @SneakyThrows
    @Test
    public void onReadError_OK() {

        File folder = tempFolder.newFolder("testListener");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemReaderListener transactionItemReaderListener = new TransactionItemReaderListener(maskPolicy);
        transactionItemReaderListener.setExecutionDate(executionDate);
        transactionItemReaderListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemReaderListener.setEnableOnErrorLogging(true);
        transactionItemReaderListener.setEnableOnErrorFileLogging(true);
        transactionItemReaderListener.setEnableAfterReadLogging(true);
        transactionItemReaderListener.setTransactionWriterService(transactionWriterService);
        transactionItemReaderListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemReaderListener.onReadError(new FlatFileParseException(
                "Parsing error at line: 1 in resource=[[file:/input]]", new Exception(), "input", 1));

        BDDMockito.verify(transactionWriterService).write(any(), any());
    }

    @SneakyThrows
    @Test
    public void onReadError_OK_NoFileWritten() {

        File folder = tempFolder.newFolder("testListener");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemReaderListener transactionItemReaderListener = new TransactionItemReaderListener(maskPolicy);
        transactionItemReaderListener.setExecutionDate(executionDate);
        transactionItemReaderListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemReaderListener.setEnableOnErrorLogging(false);
        transactionItemReaderListener.setEnableOnErrorFileLogging(false);
        transactionItemReaderListener.setEnableAfterReadLogging(true);
        transactionItemReaderListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemReaderListener.onReadError(new FlatFileParseException("Parsing error at line: " +
                1, new Exception(), "input", 1));

        BDDMockito.verify(transactionWriterService, Mockito.times(0)).write(any(), any());
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}