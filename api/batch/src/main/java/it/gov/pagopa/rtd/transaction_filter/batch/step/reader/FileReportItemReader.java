package it.gov.pagopa.rtd.transaction_filter.batch.step.reader;

import it.gov.pagopa.rtd.transaction_filter.connector.FileReportRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.model.FileMetadata;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;

public class FileReportItemReader extends CustomIteratorItemReader<FileMetadata> {

  private final FileReportRestClient restClient;

  public FileReportItemReader(FileReportRestClient restClient) {
    super(Collections.emptyList());
    this.restClient = restClient;
  }

  @BeforeStep
  public void initializeState(StepExecution stepExecution) {
    Collection<FileMetadata> files = Objects.requireNonNull(restClient.getFileReport())
        .getFilesRecentlyUploaded();
    if (files != null) {
      super.setIterable(files);
    }
  }
}
