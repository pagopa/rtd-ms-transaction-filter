package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class PGPEncrypterTaskletTest {

  private ChunkContext chunkContext;
  private StepExecution execution;
  PGPEncrypterTasklet tasklet;

  private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

  @TempDir
  Path tempDir;

  private String pathMatcherPGPFiles;

  @BeforeEach
  void setup() {
    execution = MetaDataInstanceFactory.createStepExecution();
    StepContext stepContext = new StepContext(execution);
    chunkContext = new ChunkContext(stepContext);

    tasklet = createDefaultTasklet();

    pathMatcherPGPFiles = "file:" + tempDir + File.separator + "*.pgp";
  }

  @SneakyThrows
  @Test
  void whenTaskletIsDisabledThenDoNothing() {
    tasklet.setTaskletEnabled(false);

    RepeatStatus exitStatus = tasklet.execute(new StepContribution(execution), chunkContext);

    assertThat(exitStatus).hasToString(RepeatStatus.FINISHED.toString());
    assertThat(resolver.getResources(pathMatcherPGPFiles)).isEmpty();
  }

  @SneakyThrows
  @Test
  void whenTaskletIsEnabledThenGenerateOnePgpFile() {

    RepeatStatus exitStatus = tasklet.execute(new StepContribution(execution), chunkContext);

    assertThat(exitStatus).hasToString(RepeatStatus.FINISHED.toString());
    assertThat(resolver.getResources(pathMatcherPGPFiles)).hasSize(1);
  }

  @SneakyThrows
  PGPEncrypterTasklet createDefaultTasklet() {
    String publicKey = createPublicKey();
    PGPEncrypterTasklet pgpEncrypterTasklet = new PGPEncrypterTasklet();
    pgpEncrypterTasklet.setPublicKey(publicKey);
    pgpEncrypterTasklet.setFileToEncrypt(new UrlResource(
        Files.createFile(tempDir.resolve("inputfile.csv")).toUri()));
    pgpEncrypterTasklet.setTaskletEnabled(true);
    return pgpEncrypterTasklet;
  }

  private String createPublicKey() throws IOException {
    String publicKeyPath = "file:/" + Objects.requireNonNull(
        this.getClass().getResource("/test-encrypt")).getFile() + "/publicKey.asc";
    Resource publicKeyResource = resolver.getResource(publicKeyPath);
    FileInputStream publicKeyFilePathIS = new FileInputStream(publicKeyResource.getFile());
    return IOUtils.toString(publicKeyFilePathIS);
  }

}