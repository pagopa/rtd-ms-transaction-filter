package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashMap;
import java.util.Map;


public class AcquirerIdFlyweight {

  private static Map<String, AcquirerId> cache = new HashMap<>();

  private AcquirerIdFlyweight() {
    throw new IllegalStateException("Utility class, do not instantiate directly");
  }

  public static synchronized AcquirerId createAcquirerId(String id) {
    return cache.computeIfAbsent(id, newId -> new AcquirerId(id));
  }
}

