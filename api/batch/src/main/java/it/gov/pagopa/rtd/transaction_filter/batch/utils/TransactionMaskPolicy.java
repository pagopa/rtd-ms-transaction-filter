package it.gov.pagopa.rtd.transaction_filter.batch.utils;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;

public interface TransactionMaskPolicy {

  /**
   * Apply the masking policy to a InboundTransaction object. The object is modified via side effect.
   *
   * @param transaction object to mask
   */
  void apply(InboundTransaction transaction);

  /**
   * Apply the masking policy to a transaction represented in csv format.
   * This method is used when it's not possible to parse the string into a InboundTransaction object.
   *
   * @param transactionContentAsCsv transaction in csv format with ";" as separator
   * @return the string masked
   */
  String apply(String transactionContentAsCsv);

}
