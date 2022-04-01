package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.net.URL;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
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

public class transactionChecksumTaskletTest {

    public transactionChecksumTaskletTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Mock
    private StoreService storeServiceMock;

    @SneakyThrows
    @Before
    public void setUp() {
        Mockito.reset(storeServiceMock);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @SneakyThrows
    @Test
    public void testChecksumOk() {
        String inputTrxFile = "CSTAR.99999.TRNLOG.20220204.094652.001.csv";
        String trxResourcePath = "/test-encrypt/transactions/";
        String expectedHash = "0fae450776f018583e579bdd682bd5abb25abc3abc92da139fc9305c2adee405";

        URL url = this.getClass().getResource(trxResourcePath + inputTrxFile);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(url.toString());
        Resource inputFileResource = resources[0];

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        TransactionChecksumTasklet transactionChecksumTasklet = new TransactionChecksumTasklet();
        transactionChecksumTasklet.setStoreService(storeServiceMock);
        transactionChecksumTasklet.setTaskletEnabled(true);
        transactionChecksumTasklet.setResource(inputFileResource);
        transactionChecksumTasklet.execute(new StepContribution(execution), chunkContext);

        BDDMockito.verify(storeServiceMock).storeHash(Mockito.eq(inputTrxFile), Mockito.eq(expectedHash));
    }

}