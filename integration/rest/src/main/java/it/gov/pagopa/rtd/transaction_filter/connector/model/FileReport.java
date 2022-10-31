package it.gov.pagopa.rtd.transaction_filter.connector.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileReport {

  String fileName;
  long size;
  LocalDateTime transmissionDate;
}
