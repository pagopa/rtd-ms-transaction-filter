package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
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
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class TokenItemWriterListenerTest {

    public TokenItemWriterListenerTest(){
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

        TokenItemWriterListener tokenItemWriterListener = new TokenItemWriterListener();
        tokenItemWriterListener.setExecutionDate(executionDate);
        tokenItemWriterListener.setEnableOnErrorLogging(true);
        tokenItemWriterListener.setEnableAfterWriteLogging(true);
        tokenItemWriterListener.setEnableOnErrorFileLogging(true);
        tokenItemWriterListener.setLoggingFrequency(1L);
        tokenItemWriterListener.setTransactionWriterService(transactionWriterService);
        tokenItemWriterListener.setResolver(new PathMatchingResourcePatternResolver());
        tokenItemWriterListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        tokenItemWriterListener.onWriteError(new Exception(), Collections.singletonList(InboundTokenPan
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

        TokenItemWriterListener tokenItemWriterListener = new TokenItemWriterListener();
        tokenItemWriterListener.setExecutionDate(executionDate);
        tokenItemWriterListener.setEnableOnErrorLogging(true);
        tokenItemWriterListener.setEnableAfterWriteLogging(true);
        tokenItemWriterListener.setEnableOnErrorFileLogging(false);
        tokenItemWriterListener.setLoggingFrequency(1L);
        tokenItemWriterListener.setTransactionWriterService(transactionWriterService);
        tokenItemWriterListener.setResolver(new PathMatchingResourcePatternResolver());
        tokenItemWriterListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        tokenItemWriterListener.onWriteError(new Exception(), Collections.singletonList(InboundTokenPan
                .builder().filename("test").lineNumber(1).build()));

        BDDMockito.verify(transactionWriterService,
                Mockito.times(0)).write(Mockito.any(),Mockito.any());

    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}