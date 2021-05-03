package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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

public class TokenItemReaderListenerTest {

    public TokenItemReaderListenerTest(){
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
    public void beforeStep_OK() {

        File folder = tempFolder.newFolder("testListener");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TokenItemReaderListener tokenItemReaderListener = new TokenItemReaderListener();
        tokenItemReaderListener.setExecutionDate(executionDate);
        tokenItemReaderListener.setResolver(new PathMatchingResourcePatternResolver());
        tokenItemReaderListener.setEnableOnErrorLogging(true);
        tokenItemReaderListener.setEnableOnErrorFileLogging(true);
        tokenItemReaderListener.setEnableAfterReadLogging(true);
        tokenItemReaderListener.setLoggingFrequency(1L);
        tokenItemReaderListener.setTransactionWriterService(transactionWriterService);
        tokenItemReaderListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        tokenItemReaderListener.afterRead(InboundTokenPan
                .builder().filename("test").lineNumber(1).build());

    }

    @SneakyThrows
    @Test
    public void onReadError_OK() {

        File folder = tempFolder.newFolder("testListener");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TokenItemReaderListener tokenItemReaderListener = new TokenItemReaderListener();
        tokenItemReaderListener.setExecutionDate(executionDate);
        tokenItemReaderListener.setResolver(new PathMatchingResourcePatternResolver());
        tokenItemReaderListener.setEnableOnErrorLogging(true);
        tokenItemReaderListener.setEnableOnErrorFileLogging(true);
        tokenItemReaderListener.setEnableAfterReadLogging(true);
        tokenItemReaderListener.setTransactionWriterService(transactionWriterService);
        tokenItemReaderListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        tokenItemReaderListener.onReadError(new FlatFileParseException(
                "Parsing error at line: 1 in resource=[[file:/input]]", new Exception(), "input", 1));

        BDDMockito.verify(transactionWriterService).write(Mockito.any(),Mockito.any());


    }

    @SneakyThrows
    @Test
    public void onReadError_OK_NoFileWritten() {

        File folder = tempFolder.newFolder("testListener");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TokenItemReaderListener tokenItemReaderListener = new TokenItemReaderListener();
        tokenItemReaderListener.setExecutionDate(executionDate);
        tokenItemReaderListener.setResolver(new PathMatchingResourcePatternResolver());
        tokenItemReaderListener.setEnableOnErrorLogging(false);
        tokenItemReaderListener.setEnableOnErrorFileLogging(false);
        tokenItemReaderListener.setEnableAfterReadLogging(true);
        tokenItemReaderListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        tokenItemReaderListener.onReadError(new FlatFileParseException("Parsing error at line: " +
                1, new Exception(), "input", 1));

        BDDMockito.verify(transactionWriterService,
                Mockito.times(0)).write(Mockito.any(),Mockito.any());

    }


    @After
    public void tearDown() {
        tempFolder.delete();
    }

}