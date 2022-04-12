package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashMap;
import java.util.Map;


public class AcquirerCodeFlyweight {

  private static Map<String, AcquirerCode> cache = new HashMap<>();

  private AcquirerCodeFlyweight() {
    throw new IllegalStateException("Utility class, do not instantiate directly");
  }

  public static synchronized AcquirerCode createAcquirerCode(String code) {
    return cache.computeIfAbsent(code, newCode -> new AcquirerCode(code));
  }
}

