package it.gov.pagopa.rtd.transaction_filter.service.store;

/**
 * Class encapsulating an Acquirer Code string representation.
 *
 * We encapsulate the string into an object to leverage the Flyweight pattern
 * and reduce the memory footprint of allocating potentially millions of strings,
 * many of them we expect to be the repeated many times.
 */
public class AcquirerCode {

  String code;

  public AcquirerCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return this.code;
  }
}
