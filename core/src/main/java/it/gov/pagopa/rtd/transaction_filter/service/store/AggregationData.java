package it.gov.pagopa.rtd.transaction_filter.service.store;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AggregationData {

  private short numTrx;
  private int totalAmount;
  private String vat;
  private byte posType;
  private Currency currency;

  public void incNumTrx() {
    this.numTrx += 1;
  }

  public void incTotalAmount(int amount) {
    this.totalAmount += amount;
  }

}

