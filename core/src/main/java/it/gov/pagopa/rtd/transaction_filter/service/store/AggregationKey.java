package it.gov.pagopa.rtd.transaction_filter.service.store;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class AggregationKey {

  private AcquirerCode acquirerCode;
  private String terminalId;
  private String merchantId;
  private AcquirerId acquirerId;
  private String fiscalCode;
  private AccountingDate accountingDate;
  private byte operationType;

}

