package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.*;
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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class HpanListRecoveryTaskletTest {

    public HpanListRecoveryTaskletTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    private HpanConnectorService hpanConnectorServiceMock;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @SneakyThrows
    @Before
    public void setUp() {
        Mockito.reset(hpanConnectorServiceMock);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @SneakyThrows
    @Test
    public void testRecover_OK_Recover() {
        File hpanFolder = tempFolder.newFolder("hpanDir");
        BDDMockito.doReturn(tempFolder.newFile("tempFile")).when(hpanConnectorServiceMock).getHpanList();
        HpanListRecoveryTasklet hpanListRecoveryTasklet = new HpanListRecoveryTasklet();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        hpanListRecoveryTasklet.setFileName(OffsetDateTime.now().format(fmt).concat("_hpanlist.pgp"));
        hpanListRecoveryTasklet.setHpanConnectorService(hpanConnectorServiceMock);
        hpanListRecoveryTasklet.setHpanListDirectory(hpanFolder.getAbsolutePath());
        hpanListRecoveryTasklet.setHpanFilePattern("*.pgp");
        hpanListRecoveryTasklet.setDailyRemovalTaskletEnabled(true);
        hpanListRecoveryTasklet.setRecoveryTaskletEnabled(true);
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        hpanListRecoveryTasklet.execute(new StepContribution(execution),chunkContext);
        BDDMockito.verify(hpanConnectorServiceMock).getHpanList();
        Assert.assertEquals(1, FileUtils.listFiles(hpanFolder, new String[]{"pgp"},false).size());
    }

    @SneakyThrows
    @Test
    public void testRecover_OK_NoRecover() {
        File hpanFolder = tempFolder.newFolder("hpanDir");
        tempFolder.newFile("hpanDir/hpanlist.pgp");
        HpanListRecoveryTasklet hpanListRecoveryTasklet = new HpanListRecoveryTasklet();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        hpanListRecoveryTasklet.setFileName(OffsetDateTime.now().format(fmt).concat("_hpanlist.pgp"));
        hpanListRecoveryTasklet.setHpanConnectorService(hpanConnectorServiceMock);
        hpanListRecoveryTasklet.setHpanListDirectory(hpanFolder.getAbsolutePath());
        hpanListRecoveryTasklet.setHpanFilePattern("*.pgp");
        hpanListRecoveryTasklet.setDailyRemovalTaskletEnabled(true);
        hpanListRecoveryTasklet.setRecoveryTaskletEnabled(false);
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        hpanListRecoveryTasklet.execute(new StepContribution(execution),chunkContext);
        BDDMockito.verifyNoInteractions(hpanConnectorServiceMock);
        Assert.assertEquals(1, FileUtils.listFiles(hpanFolder, new String[]{"pgp"},false).size());
    }

    @SneakyThrows
    @Test
    public void testRecover_OK_NoRecover_AlreadyDownloaded() {

        File hpanFolder = tempFolder.newFolder("hpanDir");
        BDDMockito.doReturn(tempFolder.newFile("tempFile")).when(hpanConnectorServiceMock).getHpanList();
        HpanListRecoveryTasklet hpanListRecoveryTasklet = new HpanListRecoveryTasklet();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        hpanListRecoveryTasklet.setFileName(OffsetDateTime.now().format(fmt).concat("_hpanlist.pgp"));
        hpanListRecoveryTasklet.setHpanConnectorService(hpanConnectorServiceMock);
        hpanListRecoveryTasklet.setHpanListDirectory(hpanFolder.getAbsolutePath());
        hpanListRecoveryTasklet.setHpanFilePattern("*.pgp");
        hpanListRecoveryTasklet.setDailyRemovalTaskletEnabled(true);
        hpanListRecoveryTasklet.setRecoveryTaskletEnabled(true);
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        hpanListRecoveryTasklet.execute(new StepContribution(execution),chunkContext);

        BDDMockito.verify(hpanConnectorServiceMock, Mockito.times(1)).getHpanList();
        BDDMockito.verify(hpanConnectorServiceMock, Mockito.times(1)).cleanAllTempFiles();
        Assert.assertEquals(1, FileUtils.listFiles(hpanFolder, new String[]{"pgp"},false).size());

        hpanListRecoveryTasklet.execute(new StepContribution(execution),chunkContext);
        BDDMockito.verifyNoMoreInteractions(hpanConnectorServiceMock);
        Assert.assertEquals(1, FileUtils.listFiles(hpanFolder, new String[]{"pgp"},false).size());

    }

    @SneakyThrows
    @Test
    public void testRecover_KO() {
        File hpanFolder = tempFolder.newFolder("hpanDir");
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(hpanConnectorServiceMock).getHpanList();
        HpanListRecoveryTasklet hpanListRecoveryTasklet = new HpanListRecoveryTasklet();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        hpanListRecoveryTasklet.setFileName(OffsetDateTime.now().format(fmt).concat("_hpanlist.pgp"));        hpanListRecoveryTasklet.setHpanConnectorService(hpanConnectorServiceMock);
        hpanListRecoveryTasklet.setHpanListDirectory(hpanFolder.getAbsolutePath());
        hpanListRecoveryTasklet.setHpanFilePattern("*.pgp");
        hpanListRecoveryTasklet.setDailyRemovalTaskletEnabled(true);
        hpanListRecoveryTasklet.setRecoveryTaskletEnabled(true);
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        Assert.assertThrows(Exception.class, () -> hpanListRecoveryTasklet.execute(new StepContribution(execution),chunkContext));
        BDDMockito.verify(hpanConnectorServiceMock).getHpanList();
        Assert.assertEquals(0, FileUtils.listFiles(hpanFolder, new String[]{"pgp"},false).size());
    }

    @SneakyThrows
    @Test
    public void testRecover_OK_RecoverAfterRemoval() {
        File hpanFolder = tempFolder.newFolder("hpanDir");
        File oldFile = tempFolder.newFile("hpanDir/hpanlist.pgp");
        BDDMockito.doReturn(tempFolder.newFile("tempFile")).when(hpanConnectorServiceMock).getHpanList();
        BasicFileAttributeView basicView = Files.getFileAttributeView(
                oldFile.toPath(), BasicFileAttributeView.class);
        BasicFileAttributes basicAttrs = basicView.readAttributes();
        FileTime oldTime = FileTime.from(basicAttrs.lastModifiedTime().toInstant()
                .minus(1, ChronoUnit.DAYS));
        basicView.setTimes(oldTime, oldTime, oldTime);
        HpanListRecoveryTasklet hpanListRecoveryTasklet = new HpanListRecoveryTasklet();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        hpanListRecoveryTasklet.setFileName(OffsetDateTime.now().format(fmt).concat("_hpanlist.pgp"));
        hpanListRecoveryTasklet.setHpanConnectorService(hpanConnectorServiceMock);
        hpanListRecoveryTasklet.setHpanListDirectory(hpanFolder.getAbsolutePath());
        hpanListRecoveryTasklet.setHpanFilePattern("*.pgp");
        hpanListRecoveryTasklet.setDailyRemovalTaskletEnabled(true);
        hpanListRecoveryTasklet.setRecoveryTaskletEnabled(true);
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        hpanListRecoveryTasklet.execute(new StepContribution(execution),chunkContext);
        BDDMockito.verify(hpanConnectorServiceMock).getHpanList();
        Assert.assertEquals(1, FileUtils.listFiles(hpanFolder, new String[]{"pgp"},false).size());
    }


    @After
    public void tearDown() {
        tempFolder.delete();
    }

}