package it.gov.pagopa.rtd.transaction_filter.batch.step.reader;

import it.gov.pagopa.rtd.transaction_filter.connector.FileReportRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.model.FileMetadata;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class FileReportItemReader extends CustomIteratorItemReader<FileMetadata> {

  public FileReportItemReader(FileReportRestClient restClient) {
    super(Collections.emptyList());

    Collection<FileMetadata> files = Objects.requireNonNull(restClient.getFileReport())
        .getFilesRecentlyUploaded();

    if (files != null) {
      super.setIterable(files);
    }
  }
}
