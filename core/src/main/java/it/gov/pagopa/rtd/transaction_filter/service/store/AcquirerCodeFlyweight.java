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

  private Map<String, AcquirerCode> cache;

  public AcquirerCodeFlyweight() {
    this.cache = new HashMap<>();
  }

  public synchronized AcquirerCode createAcquirerCode(String code) {
    return this.cache.computeIfAbsent(code, newCode -> new AcquirerCode(code));
  }

  public synchronized int cacheSize() {
    return this.cache.size();
  }

  public synchronized Collection<AcquirerCode> values() {
    return this.cache.values();
  }

  public synchronized void clean() {
    this.cache = new HashMap<>();
  }
}

