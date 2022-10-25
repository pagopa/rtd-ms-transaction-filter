package it.gov.pagopa.rtd.transaction_filter.batch.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RawInputLine {

  private String content;
  private int lineNumber;

  @Override
  public String toString() {
    return lineNumber + "_" + content;
  }
}
