package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.io.IOException;
import java.net.URL;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public class TransactionChecksumTaskletTest {

    private final static String inputTrxFile = "CSTAR.99999.TRNLOG.20220204.094652.001.csv";
    private final static String trxResourcePath = "/test-encrypt/transactions/";
    private final static String expectedHash = "8bca0fdabf06e1c30b716224c67a5753ac5d999cf6a375ac7adba16f725f2046";

    private Resource inputFileResource;
    private ChunkContext chunkContext;
    private StepExecution execution;

    @Mock
    private StoreService storeServiceMock;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public TransactionChecksumTaskletTest(){
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

        URL url = this.getClass().getResource(trxResourcePath + inputTrxFile);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(url.toString());
        inputFileResource = resources[0];

        execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        chunkContext = new ChunkContext(stepContext);
    }

    @Test
    public void shouldComputeHash() throws Exception {
        TransactionChecksumTasklet transactionChecksumTasklet = new TransactionChecksumTasklet();
        transactionChecksumTasklet.setStoreService(storeServiceMock);
        transactionChecksumTasklet.setTaskletEnabled(true);
        transactionChecksumTasklet.setResource(inputFileResource);
        transactionChecksumTasklet.execute(new StepContribution(execution), chunkContext);
        verify(storeServiceMock, Mockito.times(1)).setTargetInputFileHash(expectedHash);
    }

    @Test
    public void shouldNotComputeHashWhenEnabledEqualsFalse() throws Exception {
        TransactionChecksumTasklet transactionChecksumTasklet = new TransactionChecksumTasklet();
        transactionChecksumTasklet.setStoreService(storeServiceMock);
        transactionChecksumTasklet.setTaskletEnabled(false);
        transactionChecksumTasklet.setResource(inputFileResource);
        transactionChecksumTasklet.execute(new StepContribution(execution), chunkContext);
        verify(storeServiceMock, Mockito.times(0)).setTargetInputFileHash(Mockito.any());
    }

}