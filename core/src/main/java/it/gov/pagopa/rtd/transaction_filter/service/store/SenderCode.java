package it.gov.pagopa.rtd.transaction_filter.service.store;

/**
 * Class encapsulating a Sender Code string representation.
 *
 * We encapsulate the string into an object to leverage the Flyweight pattern
 * and reduce the memory footprint of allocating potentially millions of strings,
 * many of them we expect to be the repeated many times.
 */
public class SenderCode {

  String code;

  public SenderCode(String code) {
    this.code = code;
  }

  public String getCode() {
    return this.code;
  }
}
