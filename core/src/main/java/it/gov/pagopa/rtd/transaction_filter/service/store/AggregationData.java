package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class AggregationData {

  private AtomicLong numTrx = new AtomicLong(0);
  private AtomicLong totalAmount = new AtomicLong(0);
  private Set<String> currencies = new HashSet<>();
  private Set<String> vats = new HashSet<>();
  private Set<String> posTypes = new HashSet<>();

}

