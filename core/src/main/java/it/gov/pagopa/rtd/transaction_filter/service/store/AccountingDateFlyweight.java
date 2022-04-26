package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashMap;
import java.util.Map;


/**
 * To avoid the instantiation of many Strings containing potentially the same few accounting dates
 * repeated thousand of times leverage the Flyweight pattern to ensure that for each possible
 * accounting date one and only one object representing it will be created in memory.
 */
public class AccountingDateFlyweight {

  private static Map<String, AccountingDate> cache;

  public AccountingDateFlyweight() {
    this.cache = new HashMap<>();
  }

  public synchronized AccountingDate createAccountingDate(String date) {
    return this.cache.computeIfAbsent(date, newDate -> new AccountingDate(date));
  }

  public synchronized void clean() {
    this.cache = new HashMap<>();
  }
}

