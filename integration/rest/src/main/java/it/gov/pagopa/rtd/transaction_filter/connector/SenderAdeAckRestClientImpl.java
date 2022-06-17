package it.gov.pagopa.rtd.transaction_filter.connector;

import it.gov.pagopa.rtd.transaction_filter.validator.BasicResponseEntityValidator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class SenderAdeAckRestClientImpl implements SenderAdeAckRestClient {

  @Value("${rest-client.hpan.api.key}")
  private String apiKey;

  private File tempDir = FileUtils.getTempDirectory();

  private final BasicResponseEntityValidator<SenderAdeAckList> senderAdeAckValidator;
  private final BasicResponseEntityValidator<Resource> resourceValidator;
  private final HpanRestConnector hpanRestConnector;

  @Override
  public List<File> getSenderAdeAckFiles() throws IOException {
    ResponseEntity<SenderAdeAckList> senderAdeAckResponse = hpanRestConnector.getSenderAdeAckList(
        apiKey);

    senderAdeAckValidator.validate(senderAdeAckResponse);

    SenderAdeAckList senderAdeAckDto = Objects.requireNonNull(senderAdeAckResponse.getBody());
    return downloadFiles(senderAdeAckDto.getFileNameList());
  }

  private List<File> downloadFiles(List<String> fileNameList) throws IOException {
    List<File> filesDownloaded = new ArrayList<>();
    // a dedicated temporary directory will be created to save the sender ade ack files
    // until they are persisted in the correct directory
    Path temporaryDirectory = Files.createTempDirectory(tempDir.toPath(), "senderAdeAck");

    for (String fileName : fileNameList) {
      ResponseEntity<Resource> resourceResponseEntity = hpanRestConnector.getSenderAdeAckFile(
          apiKey, fileName);

      resourceValidator.validateStatus(resourceResponseEntity.getStatusCode());

      Resource resource = resourceResponseEntity.getBody();
      if (resource != null && resource.exists()) {
        File tempFile = createTempFile(fileName, temporaryDirectory);
        copyFromResourceToFile(resource, tempFile);
        filesDownloaded.add(tempFile);
      } else {
        log.warn("received empty file");
      }
    }

    return filesDownloaded;
  }

  private File createTempFile(@NotNull String filename, @NotNull Path directory)
      throws IOException {
    return Files.createFile(Paths.get(directory.toString()
        .concat("/")
        .concat(filename))).toFile();
  }

  private void copyFromResourceToFile(@NotNull Resource resource, @NotNull File file)
      throws IOException {
    try (
        FileOutputStream tempFileFOS = new FileOutputStream(file);
        InputStream inputStream = resource.getInputStream()) {
      StreamUtils.copy(inputStream, tempFileFOS);
    }
  }

  public void setTempDir(File tempDir) {
    this.tempDir = tempDir;
  }
}
