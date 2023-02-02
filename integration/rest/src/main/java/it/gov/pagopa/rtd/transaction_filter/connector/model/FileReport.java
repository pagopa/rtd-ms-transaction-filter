package it.gov.pagopa.rtd.transaction_filter.connector.model;

import java.util.List;
import lombok.Data;

@Data
public class FileReport {

  List<FileMetadata> filesRecentlyUploaded;
}
