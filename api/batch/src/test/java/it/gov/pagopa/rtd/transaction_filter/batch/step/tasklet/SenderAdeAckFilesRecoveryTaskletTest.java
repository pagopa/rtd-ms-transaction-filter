package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import it.gov.pagopa.rtd.transaction_filter.connector.SenderAdeAckRestClient;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Slf4j
class SenderAdeAckFilesRecoveryTaskletTest {

  private ChunkContext chunkContext;
  private StepExecution execution;
  private SenderAdeAckFilesRecoveryTasklet tasklet;
  private List<File> defaultResponse;

  @TempDir
  private Path temporaryOutputPath;

  @TempDir
  private Path temporarySourcePath;

  private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

  @Mock
  private SenderAdeAckRestClient restClient;

  private AutoCloseable closeable;

  @SneakyThrows
  @BeforeEach
  void setup() {
    closeable = MockitoAnnotations.openMocks(this);

    execution = MetaDataInstanceFactory.createStepExecution();
    StepContext stepContext = new StepContext(execution);
    chunkContext = new ChunkContext(stepContext);

    tasklet = createDefaultTasklet();
    defaultResponse = createDefaultFiles();
  }

  @SneakyThrows
  @AfterEach
  void cleanup() {
    closeable.close();
  }

  @SneakyThrows
  @Test
  void saveSenderAdeAckFilesCorrectly() {
    BDDMockito.doReturn(defaultResponse).when(restClient)
        .getSenderAdeAckFiles();

    RepeatStatus exitStatus = tasklet.execute(new StepContribution(execution), chunkContext);

    List<File> resources = retrieveSavedFiles();
    assertThat(exitStatus).hasToString(RepeatStatus.FINISHED.toString());
    verify(restClient, Mockito.times(1)).getSenderAdeAckFiles();
    assertThat(resources).isNotNull().isNotEmpty().hasSize(2);
  }

  @SneakyThrows
  @Test
  void savedSenderAdeAckFilenameMatchWithExpected() {
    BDDMockito.doReturn(Collections.singletonList(defaultResponse.get(0))).when(restClient)
        .getSenderAdeAckFiles();

    tasklet.execute(new StepContribution(execution), chunkContext);

    List<File> files = retrieveSavedFiles();

    assertThat(files).isNotNull().isNotEmpty();
    assertThat(files.stream().findAny().get()).hasName("senderAdeAck1.txt");
  }

  @SneakyThrows
  @Test
  void savedSenderAdeAckContentMatchWithExpected() {
    BDDMockito.doReturn(Collections.singletonList(defaultResponse.get(0))).when(restClient)
        .getSenderAdeAckFiles();

    tasklet.execute(new StepContribution(execution), chunkContext);

    List<File> files = retrieveSavedFiles();
    String fileContent = FileUtils.readFileToString(files.get(0));
    assertThat(fileContent).isEqualTo("12345;fiscalcode");
  }

  @SneakyThrows
  @Test
  void whenGetSenderAdeAckFilesReturnsEmptyListThenSaveNothing() {
    BDDMockito.doReturn(Collections.emptyList()).when(restClient).getSenderAdeAckFiles();

    tasklet.execute(new StepContribution(execution), chunkContext);

    List<File> files = retrieveSavedFiles();
    assertThat(files).isNotNull().isEmpty();
  }

  @SneakyThrows
  @Test
  void whenTaskletIsNotEnabledThenDoNothing() {
    tasklet.setTaskletEnabled(false);

    tasklet.execute(new StepContribution(execution), chunkContext);

    verify(restClient, Mockito.times(0)).getSenderAdeAckFiles();
  }

  @SneakyThrows
  @Test
  void whenFileHasNameNullThenThrowException() {
    assertThatThrownBy(() -> tasklet.createOutputFile(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @SneakyThrows
  @Test
  void whenFileIsNotFoundThenThrowException() {
    File fileNotExisting = new File("fileNotExisting");
    BDDMockito.doReturn(Collections.singletonList(fileNotExisting)).when(restClient)
        .getSenderAdeAckFiles();

    StepContribution stepContribution = new StepContribution(execution);
    assertThatThrownBy(() -> tasklet.execute(stepContribution, chunkContext))
        .isInstanceOf(FileNotFoundException.class);
  }

  @SneakyThrows
  @Test
  void whenDirectoryIsNotSetThenThrowException() {
    BDDMockito.doReturn(Collections.singletonList(defaultResponse.get(0))).when(restClient)
        .getSenderAdeAckFiles();
    tasklet.setSenderAdeAckDirectory(null);

    assertThatThrownBy(() -> tasklet.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class);
  }

  SenderAdeAckFilesRecoveryTasklet createDefaultTasklet() {
    SenderAdeAckFilesRecoveryTasklet tasklet = new SenderAdeAckFilesRecoveryTasklet(restClient);
    tasklet.setSenderAdeAckDirectory(temporaryOutputPath.toString());
    tasklet.setTaskletEnabled(true);
    return tasklet;
  }

  @SneakyThrows
  List<File> createDefaultFiles() {
    List<File> files = new ArrayList<>();
    Path firstFile = Files.createFile(temporarySourcePath.resolve("senderAdeAck1.txt"));
    byte[] firstFileContent = "12345;fiscalcode".getBytes(StandardCharsets.UTF_8);
    Files.write(firstFile, firstFileContent);
    files.add(firstFile.toFile());

    Path secondFile = Files.createFile(temporarySourcePath.resolve("senderAdeAck2.txt"));
    byte[] secondFileContent = "7890123;stringdefault".getBytes(StandardCharsets.UTF_8);
    Files.write(secondFile, secondFileContent);
    files.add(secondFile.toFile());

    return files;
  }

  @SneakyThrows
  private List<File> retrieveSavedFiles() {
    return Arrays.stream(resolver.getResources("file:/"
            .concat(temporaryOutputPath.toString())
            .concat("/")
            .concat("*")))
        .map(resource -> {
          try {
            return resource.getFile();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }).collect(Collectors.toList());
  }
}