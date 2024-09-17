package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;

@ExtendWith(MockitoExtension.class)
public class SelectTargetInputFileTaskletTest {

    private ChunkContext chunkContext;
    private StepExecution execution;

    @Mock
    private StoreService storeServiceMock;

    @TempDir
    private Path tempFolder;

    @BeforeAll
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @BeforeEach
    public void setUp() {
        execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        chunkContext = new ChunkContext(stepContext);
    }

    @Test
    void shouldQuitJobWhenNoInputFilesAreFound() {
        SelectTargetInputFileTasklet tasklet = new SelectTargetInputFileTasklet();
        tasklet.setStoreService(storeServiceMock);
        tasklet.setTransactionDirectoryPath("file:" + tempFolder.toAbsolutePath());

        assertThrows(IOException.class, () -> tasklet.execute(new StepContribution(execution), chunkContext));
        BDDMockito.verify(storeServiceMock, Mockito.times(0)).setTargetInputFile(Mockito.any());
    }

    @Test
    void shouldSelectTheOnlyOneFilePresent() throws IOException {
        String inputFilename = "CSTAR.99999.TRNLOG.20220419.065813.001.csv";
        Files.createFile(tempFolder.resolve(inputFilename));

        SelectTargetInputFileTasklet tasklet = new SelectTargetInputFileTasklet();
        tasklet.setStoreService(storeServiceMock);
        tasklet.setTransactionDirectoryPath("file:" + tempFolder.toAbsolutePath());

        tasklet.execute(new StepContribution(execution), chunkContext);
        BDDMockito.verify(storeServiceMock).setTargetInputFile(inputFilename);
    }

    @Test
    void shouldSelectTheOlderFilePresent() throws IOException {
        Files.createFile(tempFolder.resolve("CSTAR.99999.TRNLOG.20220418.065813.001.csv"));
        Files.createFile(tempFolder.resolve("CSTAR.99999.TRNLOG.20220418.065813.002.csv"));
        Files.createFile(tempFolder.resolve("CSTAR.99999.TRNLOG.20220418.122117.001.csv"));
        Files.createFile(tempFolder.resolve("CSTAR.99999.TRNLOG.20220419.065813.001.csv"));

        SelectTargetInputFileTasklet tasklet = new SelectTargetInputFileTasklet();
        tasklet.setStoreService(storeServiceMock);
        tasklet.setTransactionDirectoryPath("file:" + tempFolder.toAbsolutePath());

        tasklet.execute(new StepContribution(execution), chunkContext);
        BDDMockito.verify(storeServiceMock).setTargetInputFile("CSTAR.99999.TRNLOG.20220418.065813.001.csv");
    }

}