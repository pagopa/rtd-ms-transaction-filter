package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashMap;
import java.util.Map;


/**
 * To avoid the instantiation of many Strings containing potentially the same few acquirer ids
 * repeated thousand of times leverage the Flyweight pattern to ensure that for each possible
 * acquirer id one and only one object representing it will be created in memory.
 */
public class AcquirerIdFlyweight {

  private static Map<String, AcquirerId> cache;

  public AcquirerIdFlyweight() {
    this.cache = new HashMap<>();
  }

  public synchronized AcquirerId createAcquirerId(String id) {
    return this.cache.computeIfAbsent(id, newId -> new AcquirerId(id));
  }

  public synchronized void clean() {
    this.cache = new HashMap<>();
  }
}

