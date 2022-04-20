package it.gov.pagopa.rtd.transaction_filter.service.store;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class AggregationData {

  private int numTrx;
  // Integer should be fine until we aggregate on daily basis.
  // Remember to re-evaluate the data type in case the aggregation period would be increased.
  private int totalAmount;
  private String vat = "empty";
  private byte posType = 126;
  private Currency currency;

  public void incNumTrx() {
    this.numTrx += 1;
  }

  public void incTotalAmount(int amount) {
    this.totalAmount += amount;
  }

}

