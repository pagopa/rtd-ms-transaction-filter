package it.gov.pagopa.rtd.transaction_filter.connector;

import java.time.OffsetDateTime;
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
  OffsetDateTime availableUntil;

  @NotNull
  OffsetDateTime generationDate;

}