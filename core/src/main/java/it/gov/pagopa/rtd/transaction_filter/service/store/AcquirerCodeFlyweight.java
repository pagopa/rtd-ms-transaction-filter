package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashMap;
import java.util.Map;

public class AcquirerCodeFlyweight {

  private static Map<String, AcquirerCode> cache = new HashMap<>();

  public synchronized static AcquirerCode createAcquirerCode(String code) {
    AcquirerCode acquirerCode = cache.computeIfAbsent(code, newCode -> new AcquirerCode(code));
    return acquirerCode;
  }
}

