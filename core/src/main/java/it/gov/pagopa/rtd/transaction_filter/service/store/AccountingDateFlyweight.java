package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashMap;
import java.util.Map;


public class AccountingDateFlyweight {

  private static Map<String, AccountingDate> cache = new HashMap<>();

  private AccountingDateFlyweight() {
    throw new IllegalStateException("Utility class, do not instantiate directly");
  }

  public static synchronized AccountingDate createAccountingDate(String date) {
    return cache.computeIfAbsent(date, newDate -> new AccountingDate(date));
  }
}

