package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AggregationData {

  private short numTrx;
  private long totalAmount;
  private String vat;
  private byte posType;

  public void incNumTrx() {
    this.numTrx += 1;
  }

  public void incTotalAmount(long amount) {
    this.totalAmount += amount;
  }

}

