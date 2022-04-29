package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerCodeFlyweight;
import java.io.IOException;
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


public class EnforceAcquirerCodeUniquenessTaskletTest {

    private ChunkContext chunkContext;
    private StepExecution execution;

    @Mock
    private StoreService storeServiceMock;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public EnforceAcquirerCodeUniquenessTaskletTest(){
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

        execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        chunkContext = new ChunkContext(stepContext);
    }

    @Test
    public void shouldRaiseIOExceptionWhenCacheIsEmpty() throws IOException {
        EnforceAcquirerCodeUniquenessTasklet tasklet = new EnforceAcquirerCodeUniquenessTasklet();
        tasklet.setStoreService(storeServiceMock);

        AcquirerCodeFlyweight flyweightMock = new AcquirerCodeFlyweight();
        BDDMockito.doReturn(flyweightMock).when(storeServiceMock).getAcquirerCodeFlyweight();

        expectedException.expect(IOException.class);
        tasklet.execute(new StepContribution(execution), chunkContext);

        verify(storeServiceMock, Mockito.times(1)).getAcquirerCodeFlyweight();
    }

    @Test
    public void shouldRaiseIOExceptionWhenCacheHasMoreThanOneValue() throws IOException {
        EnforceAcquirerCodeUniquenessTasklet tasklet = new EnforceAcquirerCodeUniquenessTasklet();
        tasklet.setStoreService(storeServiceMock);

        AcquirerCodeFlyweight flyweightMock = new AcquirerCodeFlyweight();
        flyweightMock.createAcquirerCode("11111");
        flyweightMock.createAcquirerCode("22222");
        BDDMockito.doReturn(flyweightMock).when(storeServiceMock).getAcquirerCodeFlyweight();

        expectedException.expect(IOException.class);
        tasklet.execute(new StepContribution(execution), chunkContext);

        verify(storeServiceMock, Mockito.times(1)).getAcquirerCodeFlyweight();
    }

    @Test
    public void shouldRaiseIOExceptionWhenCachedValueIsDifferentFromFilenamePart() throws IOException {
        EnforceAcquirerCodeUniquenessTasklet tasklet = new EnforceAcquirerCodeUniquenessTasklet();
        tasklet.setStoreService(storeServiceMock);

        AcquirerCodeFlyweight flyweightMock = new AcquirerCodeFlyweight();
        flyweightMock.createAcquirerCode("11111");
        BDDMockito.doReturn(flyweightMock).when(storeServiceMock).getAcquirerCodeFlyweight();
        BDDMockito.doReturn("22222").when(storeServiceMock).getTargetInputFileAbiPart();

        expectedException.expect(IOException.class);
        tasklet.execute(new StepContribution(execution), chunkContext);

        verify(storeServiceMock, Mockito.times(1)).getAcquirerCodeFlyweight();
        verify(storeServiceMock, Mockito.times(1)).getTargetInputFileAbiPart();
    }

    @Test
    public void shouldReturnSuccessWhenUniquenessIsVerified() throws IOException {
        EnforceAcquirerCodeUniquenessTasklet tasklet = new EnforceAcquirerCodeUniquenessTasklet();
        tasklet.setStoreService(storeServiceMock);

        String abiCode = "11111";
        AcquirerCodeFlyweight flyweightMock = new AcquirerCodeFlyweight();
        flyweightMock.createAcquirerCode(abiCode);
        BDDMockito.doReturn(flyweightMock).when(storeServiceMock).getAcquirerCodeFlyweight();
        BDDMockito.doReturn(abiCode).when(storeServiceMock).getTargetInputFileAbiPart();

        tasklet.execute(new StepContribution(execution), chunkContext);

        verify(storeServiceMock, Mockito.times(1)).getAcquirerCodeFlyweight();
        verify(storeServiceMock, Mockito.times(1)).getTargetInputFileAbiPart();
    }
}