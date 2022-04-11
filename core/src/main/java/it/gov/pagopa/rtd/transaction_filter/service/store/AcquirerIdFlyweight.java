package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashMap;
import java.util.Map;

public class AcquirerIdFlyweight {

  private static Map<String, AcquirerId> cache = new HashMap<>();

  public synchronized static AcquirerId createAcquirerId(String id) {
    AcquirerId acquirerId = cache.computeIfAbsent(id, newId -> new AcquirerId(id));
    return acquirerId;
  }
}

