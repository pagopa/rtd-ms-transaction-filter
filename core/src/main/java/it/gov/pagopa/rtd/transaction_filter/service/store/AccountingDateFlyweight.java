package it.gov.pagopa.rtd.transaction_filter.service.store;

import java.util.HashMap;
import java.util.Map;

public class AccountingDateFlyweight {

  private static Map<String, AccountingDate> cache = new HashMap<>();

  public synchronized static AccountingDate createAccountingDate(String date) {
    AccountingDate accountingDate = cache.computeIfAbsent(date, newDate -> new AccountingDate(date));
    return accountingDate;
  }
}

