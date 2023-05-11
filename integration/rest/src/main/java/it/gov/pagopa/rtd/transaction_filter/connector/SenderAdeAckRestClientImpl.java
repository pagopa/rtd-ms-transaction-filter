package it.gov.pagopa.rtd.transaction_filter.connector;

import feign.FeignException;
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
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ResponseStatusException;

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
    SenderAdeAckList adeAckList = retrieveAdeAckList();

    return downloadFiles(adeAckList.getFileNameList());
  }

  private SenderAdeAckList retrieveAdeAckList() {
    SenderAdeAckList senderAdeAckList;
    try {
      ResponseEntity<SenderAdeAckList> adeAckResponse = hpanRestConnector.getSenderAdeAckList(
          apiKey);
      senderAdeAckValidator.validate(adeAckResponse);

      senderAdeAckList = adeAckResponse.getBody();
    } catch (FeignException | ResponseStatusException ex) {
      log.warn("Failed to download ade ack list! It will be downloaded on the next run.");
      // returns an empty list
      return new SenderAdeAckList();
    }

    return Objects.requireNonNull(senderAdeAckList);
  }

  private List<File> downloadFiles(List<String> fileNameList) throws IOException {
    List<File> filesDownloaded = new ArrayList<>();
    // a dedicated temporary directory will be created to save the sender ade ack files
    // until they are persisted in the correct directory
    Path temporaryDirectory = Files.createTempDirectory(tempDir.toPath(), "senderAdeAck");

    for (String fileName : fileNameList) {
      Optional<Resource> resource = downloadAdeAck(fileName);
      if (!resource.isPresent()) {
        // skip to next file
        continue;
      }

      File tempFile = createTempFile(fileName, temporaryDirectory);
      copyFromResourceToFile(resource.get(), tempFile);

      boolean isDownloadConfirmed = sendAckReceivedConfirmation(fileName);
      if (isDownloadConfirmed) {
        filesDownloaded.add(tempFile);
      } else {
        // if the download confirmation failed then the file is deleted and must be downloaded again
        FileUtils.deleteQuietly(tempFile);
      }
    }

    // if any files has been downloaded then delete temp folder
    if (filesDownloaded.isEmpty()) {
      FileUtils.deleteDirectory(temporaryDirectory.toFile());
    }

    return filesDownloaded;
  }

  private Optional<Resource> downloadAdeAck(String fileName) {
    Resource adeAckResource = null;
    try {
      ResponseEntity<Resource> resourceResponseEntity = hpanRestConnector.getSenderAdeAckFile(
          apiKey, fileName);
      resourceValidator.validateStatus(resourceResponseEntity.getStatusCode());

      adeAckResource = resourceResponseEntity.getBody();
    } catch (FeignException | ResponseStatusException ex) {
      log.warn("Failed to download the ack: {}! It will be downloaded on the next run.", fileName);
    }

    return Optional.ofNullable(adeAckResource);
  }

  private File createTempFile(@NotNull String filename, @NotNull Path directory)
      throws IOException {
    return Files.createFile(Paths.get(directory.toString()
        .concat("/")
        .concat(filename))).toFile();
  }

  private void copyFromResourceToFile(@NotNull Resource resource, @NotNull File file)
      throws IOException {
    try (FileOutputStream tempFileFOS = new FileOutputStream(file);
        InputStream inputStream = resource.getInputStream()) {
      StreamUtils.copy(inputStream, tempFileFOS);
    }
  }

  private boolean sendAckReceivedConfirmation(String fileName) {
    try {
      ResponseEntity<Void> responseEntity = hpanRestConnector.putAckReceived(apiKey, fileName, "");
      resourceValidator.validateStatus(responseEntity.getStatusCode());
    } catch (FeignException | ResponseStatusException ex) {
      log.warn("Cannot confirm {} file download! It will be downloaded on the next run.", fileName);
      return false;
    }
    return true;
  }

  public void setTempDir(File tempDir) {
    this.tempDir = tempDir;
  }
}
