package it.gov.pagopa.rtd.transaction_filter.service.store;

/**
 * Class encapsulating a Currency string representation.
 *
 * We encapsulate the string into an object to leverage the Flyweight pattern
 * and reduce the memory footprint of allocating potentially millions of strings,
 * many of them we expect to be the repeated many times.
 */
public class Currency {

  String isoCode;

  public Currency(String isoCode) {
    this.isoCode = isoCode;
  }

  public String getIsoCode() {
    return this.isoCode;
  }
}
