package it.gov.pagopa.rtd.transaction_filter.connector.model;

import java.util.Collection;
import lombok.Data;

@Data
public class FileReport {

  Collection<FileMetadata> filesReceivedFromSender;
}
