package it.gov.pagopa.rtd.transaction_filter.connector;

import it.gov.pagopa.rtd.transaction_filter.connector.model.FileMetadata;
import it.gov.pagopa.rtd.transaction_filter.connector.model.FileReport;
import it.gov.pagopa.rtd.transaction_filter.validator.BasicResponseEntityValidator;
import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
class FileReportRestClientImpl implements FileReportRestClient {

  @Value("${rest-client.hpan.api.key}")
  private String apiKey;
  private final HpanRestConnector hpanRestConnector;
  private final BasicResponseEntityValidator<FileReport> reportValidator;
  private final Validator fileValidator;

  @Override
  public FileReport getFileReport() {
    ResponseEntity<FileReport> fileReportResponse = hpanRestConnector.getFileReport(apiKey);

    reportValidator.validate(fileReportResponse);
    for (FileMetadata file : Objects.requireNonNull(fileReportResponse.getBody()).getFilesRecentlyUploaded()) {
      validateFileMetadata(file);
    }

    return fileReportResponse.getBody();
  }

  private void validateFileMetadata(FileMetadata fileToValidate) {
    Set<ConstraintViolation<FileMetadata>> violations = fileValidator.validate(fileToValidate);

    if (!violations.isEmpty()) {
      log.error("Validation errors in FileReport.");
      throw new ConstraintViolationException(violations);
    }
  }
}
