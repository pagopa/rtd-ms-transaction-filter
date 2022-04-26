package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * To avoid the instantiation of many Strings containing potentially the same few acquirer codes
 * repeated thousand of times leverage the Flyweight pattern to ensure that for each possible
 * acquirer code one and only one object representing it will be created in memory.
 */
public class AcquirerCodeFlyweight {

  private static Map<String, AcquirerCode> cache = new HashMap<>();

  private AcquirerCodeFlyweight() {
    throw new IllegalStateException("Utility class, do not instantiate directly");
  }

  public static synchronized AcquirerCode createAcquirerCode(String code) {
    return cache.computeIfAbsent(code, newCode -> new AcquirerCode(code));
  }

  public static synchronized int cacheSize() {
    return cache.size();
  }

  public static synchronized Collection<AcquirerCode> values() {
    return cache.values();
  }

  public static synchronized void clean() {
    cache = new HashMap<>();
  }
}

