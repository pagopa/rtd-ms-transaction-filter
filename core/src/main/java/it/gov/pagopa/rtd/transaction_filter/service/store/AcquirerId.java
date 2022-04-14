package it.gov.pagopa.rtd.transaction_filter.service.store;

/**
 * Class encapsulating an Acquirer ID string representation.
 *
 * We encapsulate the string into an object to leverage the Flyweight pattern
 * and reduce the memory footprint of allocating potentially millions of strings,
 * many of them we expect to be the repeated many times.
 */
public class AcquirerId {

  String id;

  public AcquirerId(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }
}
