package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.config.LoggerRule;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class TransactionItemProcessListenerTest {

    public TransactionItemProcessListenerTest(){
        MockitoAnnotations.initMocks(this);
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @Rule
    public final LoggerRule loggerRule = new LoggerRule();

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
    public void afterProcess_OK() {

        File folder = tempFolder.newFolder("testProcess");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemProcessListener transactionItemProcessListener = new TransactionItemProcessListener();
        transactionItemProcessListener.setExecutionDate(executionDate);
        transactionItemProcessListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemProcessListener.setEnableOnErrorLogging(true);
        transactionItemProcessListener.setEnableAfterProcessLogging(true);
        transactionItemProcessListener.setEnableAfterProcessFileLogging(true);
        transactionItemProcessListener.setEnableOnErrorFileLogging(true);
        transactionItemProcessListener.setEnableAfterProcessLogging(true);
        transactionItemProcessListener.setLoggingFrequency(1L);
        transactionItemProcessListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemProcessListener.setTransactionWriterService(transactionWriterService);
        transactionItemProcessListener.afterProcess(
                InboundTransaction.builder().filename("test").lineNumber(1).build(),
                null);

        BDDMockito.verify(transactionWriterService).write(Mockito.any(),Mockito.any());


    }

    @SneakyThrows
    @Test
    public void onProcessError_OK() {

        File folder = tempFolder.newFolder("testProcess");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemProcessListener transactionItemProcessListener = new TransactionItemProcessListener();
        transactionItemProcessListener.setExecutionDate(executionDate);
        transactionItemProcessListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemProcessListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemProcessListener.setEnableOnErrorLogging(true);
        transactionItemProcessListener.setEnableAfterProcessLogging(true);
        transactionItemProcessListener.setEnableOnErrorFileLogging(true);
        transactionItemProcessListener.setLoggingFrequency(1L);
        transactionItemProcessListener.setTransactionWriterService(transactionWriterService);
        transactionItemProcessListener.onProcessError(
                InboundTransaction.builder().filename("test").lineNumber(1).build(),
                new Exception());

        BDDMockito.verify(transactionWriterService).write(Mockito.any(),Mockito.any());

    }

    @SneakyThrows
    @Test
    public void onProcessError_OK_NoFileWritten() {

        File folder = tempFolder.newFolder("testProcess");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemProcessListener transactionItemProcessListener = new TransactionItemProcessListener();
        transactionItemProcessListener.setExecutionDate(executionDate);
        transactionItemProcessListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemProcessListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemProcessListener.setEnableOnErrorLogging(true);
        transactionItemProcessListener.setEnableAfterProcessLogging(true);
        transactionItemProcessListener.setEnableOnErrorFileLogging(false);
        transactionItemProcessListener.setLoggingFrequency(1L);
        transactionItemProcessListener.setTransactionWriterService(transactionWriterService);
        transactionItemProcessListener.onProcessError(
                InboundTransaction.builder().filename("test").lineNumber(1).build(),
                new Exception());

        BDDMockito.verify(transactionWriterService, Mockito.times(0)).write(Mockito.any(),Mockito.any());

    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}