package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashMap;
import java.util.Map;

public class CurrencyFlyweight {

  private static Map<String, Currency> cache = new HashMap<>();

  private CurrencyFlyweight() {
    throw new IllegalStateException("Utility class, do not instantiate directly");
  }

  public static synchronized Currency createCurrency(String isoCode) {
    return cache.computeIfAbsent(isoCode, newDate -> new Currency(isoCode));
  }
}

