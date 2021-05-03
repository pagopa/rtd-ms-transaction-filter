package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.config.LoggerRule;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
public class TokenItemProcessListenerTest {

    public TokenItemProcessListenerTest(){
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

        TokenItemProcessListener tokenItemProcessListener = new TokenItemProcessListener();
        tokenItemProcessListener.setExecutionDate(executionDate);
        tokenItemProcessListener.setResolver(new PathMatchingResourcePatternResolver());
        tokenItemProcessListener.setEnableOnErrorLogging(true);
        tokenItemProcessListener.setEnableAfterProcessLogging(true);
        tokenItemProcessListener.setEnableAfterProcessFileLogging(true);
        tokenItemProcessListener.setEnableOnErrorFileLogging(true);
        tokenItemProcessListener.setEnableAfterProcessLogging(true);
        tokenItemProcessListener.setLoggingFrequency(1L);
        tokenItemProcessListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        tokenItemProcessListener.setTransactionWriterService(transactionWriterService);
        tokenItemProcessListener.afterProcess(
                InboundTokenPan.builder().filename("test").lineNumber(1).build(),
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

        TokenItemProcessListener tokenItemProcessListener = new TokenItemProcessListener();
        tokenItemProcessListener.setExecutionDate(executionDate);
        tokenItemProcessListener.setResolver(new PathMatchingResourcePatternResolver());
        tokenItemProcessListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        tokenItemProcessListener.setEnableOnErrorLogging(true);
        tokenItemProcessListener.setEnableAfterProcessLogging(true);
        tokenItemProcessListener.setEnableOnErrorFileLogging(true);
        tokenItemProcessListener.setLoggingFrequency(1L);
        tokenItemProcessListener.setTransactionWriterService(transactionWriterService);
        tokenItemProcessListener.onProcessError(
                InboundTokenPan.builder().filename("test").lineNumber(1).build(),
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

        TokenItemProcessListener tokenItemProcessListener = new TokenItemProcessListener();
        tokenItemProcessListener.setExecutionDate(executionDate);
        tokenItemProcessListener.setResolver(new PathMatchingResourcePatternResolver());
        tokenItemProcessListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        tokenItemProcessListener.setEnableOnErrorLogging(true);
        tokenItemProcessListener.setEnableAfterProcessLogging(true);
        tokenItemProcessListener.setEnableOnErrorFileLogging(false);
        tokenItemProcessListener.setLoggingFrequency(1L);
        tokenItemProcessListener.setTransactionWriterService(transactionWriterService);
        tokenItemProcessListener.onProcessError(
                InboundTokenPan.builder().filename("test").lineNumber(1).build(),
                new Exception());

        BDDMockito.verify(transactionWriterService,
                Mockito.times(0)).write(Mockito.any(),Mockito.any());

    }


    @After
    public void tearDown() {
        tempFolder.delete();
    }

}