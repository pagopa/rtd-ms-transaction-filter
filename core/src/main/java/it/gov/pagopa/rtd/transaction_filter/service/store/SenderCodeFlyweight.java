package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * To avoid the instantiation of many Strings containing potentially the same few sender codes
 * repeated thousand of times leverage the Flyweight pattern to ensure that for each possible
 * sender code one and only one object representing it will be created in memory.
 */
public class SenderCodeFlyweight {

  private Map<String, SenderCode> cache;

  public SenderCodeFlyweight() {
    this.cache = new HashMap<>();
  }

  public synchronized SenderCode createSenderCode(String code) {
    return this.cache.computeIfAbsent(code, newCode -> new SenderCode(code));
  }

  public synchronized int cacheSize() {
    return this.cache.size();
  }

  public synchronized Collection<SenderCode> values() {
    return this.cache.values();
  }

  public synchronized void clean() {
    this.cache = new HashMap<>();
  }
}

