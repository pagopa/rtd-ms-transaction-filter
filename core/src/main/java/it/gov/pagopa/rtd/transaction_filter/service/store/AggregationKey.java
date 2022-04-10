package it.gov.pagopa.rtd.transaction_filter.service.store;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class AggregationKey {

  private String acquirerCode;
  private String terminalId;
  private String merchantId;
  private String acquirerId;
  private String fiscalCode;
  private String accountingDate;
  private byte operationType;

}

