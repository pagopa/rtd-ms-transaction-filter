package it.gov.pagopa.rtd.transaction_filter.connector.model;

import java.util.Collection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileReport {

  Collection<FileMetadata> files;
}
