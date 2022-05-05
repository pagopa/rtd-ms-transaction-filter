package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import java.io.IOException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;


public class PreventReprocessingFilenameAlreadySeenTaskletTest {

    private final static String inputTrxFile = "CSTAR.99999.TRNLOG.20220204.094652.001.csv";

    private ChunkContext chunkContext;
    private StepExecution execution;

    @Mock
    private StoreService storeServiceMock;

    @Mock
    private TransactionWriterService transactionWriterServiceMock;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public PreventReprocessingFilenameAlreadySeenTaskletTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Before
    public void setUp() throws IOException {
        reset(storeServiceMock);
        reset(transactionWriterServiceMock);

        execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        chunkContext = new ChunkContext(stepContext);
    }

    @Test
    public void shouldRaiseIOExceptionWhenFileChannelHasBeenOpenedBefore() throws IOException {
        PreventReprocessingFilenameAlreadySeenTasklet tasklet = new PreventReprocessingFilenameAlreadySeenTasklet();
        tasklet.setStoreService(storeServiceMock);
        tasklet.setTransactionWriterService(transactionWriterServiceMock);

        doReturn(inputTrxFile).when(storeServiceMock).getTargetInputFile();
        doReturn(true).when(transactionWriterServiceMock).existFileChannelForFilename(inputTrxFile);

        expectedException.expect(IOException.class);
        tasklet.execute(new StepContribution(execution), chunkContext);

        verify(storeServiceMock, Mockito.times(1)).getTargetInputFile();
    }

    @Test
    public void shouldReturnFinishedWhenFileChannelHasntBeenOpenedBefore() throws IOException {
        PreventReprocessingFilenameAlreadySeenTasklet tasklet = new PreventReprocessingFilenameAlreadySeenTasklet();
        tasklet.setStoreService(storeServiceMock);
        tasklet.setTransactionWriterService(transactionWriterServiceMock);

        doReturn(inputTrxFile).when(storeServiceMock).getTargetInputFile();
        doReturn(false).when(transactionWriterServiceMock).existFileChannelForFilename(inputTrxFile);

        tasklet.execute(new StepContribution(execution), chunkContext);

        verify(storeServiceMock, Mockito.times(1)).getTargetInputFile();
        verify(transactionWriterServiceMock, Mockito.times(1)).existFileChannelForFilename(inputTrxFile);
    }
}