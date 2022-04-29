package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.mockito.Mockito.reset;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.io.IOException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
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


public class SelectTargetInputFileTaskletTest {

    private ChunkContext chunkContext;
    private StepExecution execution;

    @Mock
    private StoreService storeServiceMock;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    public SelectTargetInputFileTaskletTest(){
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
    public void shouldQuitJobWhenNoInputFilesAreFound() throws IOException {
        SelectTargetInputFileTasklet tasklet = new SelectTargetInputFileTasklet();
        tasklet.setStoreService(storeServiceMock);
        tasklet.setTransactionDirectoryPath("file:" + tempFolder.getRoot().getAbsolutePath());

        expectedException.expect(IOException.class);
        tasklet.execute(new StepContribution(execution), chunkContext);
        BDDMockito.verify(storeServiceMock, Mockito.times(0)).setTargetInputFile(Mockito.any());
    }

    @Test
    public void shouldSelectTheOnlyOneFilePresent() throws IOException {
        String inputFilename = "CSTAR.99999.TRNLOG.20220419.065813.001.csv";
        tempFolder.newFile(inputFilename);

        SelectTargetInputFileTasklet tasklet = new SelectTargetInputFileTasklet();
        tasklet.setStoreService(storeServiceMock);
        tasklet.setTransactionDirectoryPath("file:" + tempFolder.getRoot().getAbsolutePath());

        tasklet.execute(new StepContribution(execution), chunkContext);
        BDDMockito.verify(storeServiceMock, Mockito.times(1)).setTargetInputFile(inputFilename);
    }

    @Test
    public void shouldSelectTheOlderFilePresent() throws IOException {
        tempFolder.newFile("CSTAR.99999.TRNLOG.20220418.065813.001.csv");
        tempFolder.newFile("CSTAR.99999.TRNLOG.20220418.065813.002.csv");
        tempFolder.newFile("CSTAR.99999.TRNLOG.20220418.122117.001.csv");
        tempFolder.newFile("CSTAR.99999.TRNLOG.20220419.065813.001.csv");

        SelectTargetInputFileTasklet tasklet = new SelectTargetInputFileTasklet();
        tasklet.setStoreService(storeServiceMock);
        tasklet.setTransactionDirectoryPath("file:" + tempFolder.getRoot().getAbsolutePath());

        tasklet.execute(new StepContribution(execution), chunkContext);
        BDDMockito.verify(storeServiceMock, Mockito.times(1)).setTargetInputFile("CSTAR.99999.TRNLOG.20220418.065813.001.csv");
    }

}