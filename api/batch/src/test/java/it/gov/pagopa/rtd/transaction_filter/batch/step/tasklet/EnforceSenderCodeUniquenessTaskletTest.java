package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.store.SenderCodeFlyweight;
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


public class EnforceSenderCodeUniquenessTaskletTest {

    private ChunkContext chunkContext;
    private StepExecution execution;

    @Mock
    private StoreService storeServiceMock;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public EnforceSenderCodeUniquenessTaskletTest(){
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
        EnforceSenderCodeUniquenessTasklet tasklet = new EnforceSenderCodeUniquenessTasklet();
        tasklet.setStoreService(storeServiceMock);

        SenderCodeFlyweight flyweightMock = new SenderCodeFlyweight();
        BDDMockito.doReturn(flyweightMock).when(storeServiceMock).getSenderCodeFlyweight();

        expectedException.expect(IOException.class);
        tasklet.execute(new StepContribution(execution), chunkContext);

        verify(storeServiceMock, Mockito.times(1)).getSenderCodeFlyweight();
    }

    @Test
    public void shouldRaiseIOExceptionWhenCacheHasMoreThanOneValue() throws IOException {
        EnforceSenderCodeUniquenessTasklet tasklet = new EnforceSenderCodeUniquenessTasklet();
        tasklet.setStoreService(storeServiceMock);

        SenderCodeFlyweight flyweightMock = new SenderCodeFlyweight();
        flyweightMock.createSenderCode("11111");
        flyweightMock.createSenderCode("22222");
        BDDMockito.doReturn(flyweightMock).when(storeServiceMock).getSenderCodeFlyweight();

        expectedException.expect(IOException.class);
        tasklet.execute(new StepContribution(execution), chunkContext);

        verify(storeServiceMock, Mockito.times(1)).getSenderCodeFlyweight();
    }

    @Test
    public void shouldRaiseIOExceptionWhenCachedValueIsDifferentFromFilenamePart() throws IOException {
        EnforceSenderCodeUniquenessTasklet tasklet = new EnforceSenderCodeUniquenessTasklet();
        tasklet.setStoreService(storeServiceMock);

        SenderCodeFlyweight flyweightMock = new SenderCodeFlyweight();
        flyweightMock.createSenderCode("11111");
        BDDMockito.doReturn(flyweightMock).when(storeServiceMock).getSenderCodeFlyweight();
        BDDMockito.doReturn("22222").when(storeServiceMock).getTargetInputFileAbiPart();

        expectedException.expect(IOException.class);
        tasklet.execute(new StepContribution(execution), chunkContext);

        verify(storeServiceMock, Mockito.times(1)).getSenderCodeFlyweight();
        verify(storeServiceMock, Mockito.times(1)).getTargetInputFileAbiPart();
    }

    @Test
    public void shouldReturnSuccessWhenUniquenessIsVerified() throws IOException {
        EnforceSenderCodeUniquenessTasklet tasklet = new EnforceSenderCodeUniquenessTasklet();
        tasklet.setStoreService(storeServiceMock);

        String abiCode = "11111";
        SenderCodeFlyweight flyweightMock = new SenderCodeFlyweight();
        flyweightMock.createSenderCode(abiCode);
        BDDMockito.doReturn(flyweightMock).when(storeServiceMock).getSenderCodeFlyweight();
        BDDMockito.doReturn(abiCode).when(storeServiceMock).getTargetInputFileAbiPart();

        tasklet.execute(new StepContribution(execution), chunkContext);

        verify(storeServiceMock, Mockito.times(1)).getSenderCodeFlyweight();
        verify(storeServiceMock, Mockito.times(1)).getTargetInputFileAbiPart();
    }
}