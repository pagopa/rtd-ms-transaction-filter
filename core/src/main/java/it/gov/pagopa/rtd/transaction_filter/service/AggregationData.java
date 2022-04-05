package it.gov.pagopa.rtd.transaction_filter.service;

import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class AggregationData {

  private AtomicLong numTrx = new AtomicLong(0);
  private AtomicLong totalAmount = new AtomicLong(0);

}

