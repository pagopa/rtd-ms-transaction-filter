package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.connector.SenderAdeAckRestClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;

/**
 * implementation of the {@link Tasklet}, recovers the sender ade ack files from a REST service,
 * when enabled
 */

@Slf4j
@Data
@RequiredArgsConstructor
public class SenderAdeAckFilesRecoveryTasklet implements Tasklet, InitializingBean {

  private final SenderAdeAckRestClient restClient;
  private String senderAdeAckDirectory;
  private boolean taskletEnabled = false;

  PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

  /**
   * Retrieves and persist in an output directory the files containing the sender ade ack.
   *
   * @return task exit status
   */
  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
      throws IOException {
    makePathSystemIndependent();

    if (taskletEnabled) {
      List<File> senderAdeAckTemporaryFiles = restClient.getSenderAdeAckFiles();

      Optional<Path> parentTempDirectory = getParentTemporaryDirectory(senderAdeAckTemporaryFiles);
      saveFilesToOutputDirectory(senderAdeAckTemporaryFiles);
      parentTempDirectory.ifPresent(this::cleanupTemporaryFiles);
    }

    return RepeatStatus.FINISHED;
  }

  void makePathSystemIndependent() {
    senderAdeAckDirectory = senderAdeAckDirectory.replace("\\\\", "/");
  }

  private Optional<Path> getParentTemporaryDirectory(List<File> senderAdeAckFiles) {
    return senderAdeAckFiles.stream().map(file -> file.getParentFile().toPath()).findAny();
  }

  private void saveFilesToOutputDirectory(List<File> senderAdeAckFiles) throws IOException {
    for (File sourceFile : senderAdeAckFiles) {
      File outputFile = createOutputFile(sourceFile.getName());
      FileUtils.moveFile(sourceFile, outputFile);
    }
  }

  private File createOutputFile(String name) throws IOException {
    // it's supposed the output directory already exists
    String rootPath = resolver.getResource(getPathToResolve(senderAdeAckDirectory)).getFile()
        .getAbsolutePath();

    return FileUtils.getFile(rootPath
        .concat("/")
        .concat(name != null ? name : createRandomName()));
  }

  private String getPathToResolve(String senderAdeAckDirectory) {
    return senderAdeAckDirectory.startsWith("/") ? "file:".concat(senderAdeAckDirectory)
        : senderAdeAckDirectory;
  }

  private void cleanupTemporaryFiles(Path sourceDirectory) {
    if (sourceDirectory != null) {
      try {
        FileUtils.deleteDirectory(sourceDirectory.toFile());
      } catch (IOException e) {
        log.warn("Couldn't delete temporary files or directory");
      }
    }
  }

  private String createRandomName() {
    return "adeAck".concat(RandomStringUtils.random(16, true, true)).concat(".csv");
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(resolver.getResources(senderAdeAckDirectory), "directory must be set");
  }
}
