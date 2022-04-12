package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashMap;
import java.util.Map;

/**
 * To avoid the instantiation of many Strings containing potentially the same few currency codes
 * repeated thousand of times leverage the Flyweight pattern to ensure that for each possible
 * currency code one and only one object representing it will be created in memory.
 */
public class CurrencyFlyweight {

  private static Map<String, Currency> cache = new HashMap<>();

  private CurrencyFlyweight() {
    throw new IllegalStateException("Utility class, do not instantiate directly");
  }

  public static synchronized Currency createCurrency(String isoCode) {
    return cache.computeIfAbsent(isoCode, newDate -> new Currency(isoCode));
  }
}

