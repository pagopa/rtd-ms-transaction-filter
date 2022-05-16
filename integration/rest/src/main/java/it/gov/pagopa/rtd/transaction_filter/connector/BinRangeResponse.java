package it.gov.pagopa.rtd.transaction_filter.connector;

import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BinRangeResponse {

  @NotNull
  List<String> fileLinks;
  @NotNull
  Integer numberOfFiles;
  @NotNull
  LocalDateTime availableUntil;
  @NotNull
  LocalDateTime generationDate;

}