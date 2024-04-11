package it.gov.pagopa.rtd.transaction_filter.connector.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AggregatesDataSummary {

  private LocalDate minAccountingDate;
  private LocalDate maxAccountingDate;
  private int numberOfMerchants;
  private long countNegativeTransactions;
  private long countPositiveTransactions;
  private long sumAmountNegativeTransactions;
  private long sumAmountPositiveTransactions;
  // sha256 of the initial input file containing the transactions
  private String sha256OriginFile;

  public static AggregatesDataSummary createInvalidDataSummary() {
    return AggregatesDataSummary.builder()
        .sumAmountPositiveTransactions(-1)
        .sumAmountNegativeTransactions(-1)
        .countPositiveTransactions(-1)
        .countNegativeTransactions(-1)
        .numberOfMerchants(-1)
        .minAccountingDate(null)
        .maxAccountingDate(null)
        .build();
  }
}