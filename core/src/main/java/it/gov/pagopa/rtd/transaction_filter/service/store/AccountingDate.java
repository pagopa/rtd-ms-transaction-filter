package it.gov.pagopa.rtd.transaction_filter.service.store;

/**
 * Class encapsulating an Accounting Date string representation.
 *
 * We encapsulate the string into an object to leverage the Flyweight pattern
 * and reduce the memory footprint of allocating potentially millions of strings,
 * many of them we expect to be the repeated many times.
 */
public class AccountingDate {

  String date;

  public AccountingDate(String date) {
    this.date = date;
  }

  public String getDate() {
    return this.date;
  }
}
