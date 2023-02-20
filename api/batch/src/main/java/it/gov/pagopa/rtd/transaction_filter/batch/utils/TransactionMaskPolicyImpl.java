package it.gov.pagopa.rtd.transaction_filter.batch.utils;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

@Slf4j
public class TransactionMaskPolicyImpl implements TransactionMaskPolicy {

  private static final int PAN_INDEX = 3;

  @Override
  public void apply(InboundTransaction inboundTransaction) {

    String maskedPan = maskPan(inboundTransaction.getPan());
    inboundTransaction.setPan(maskedPan);
  }

  @Override
  public String apply(@NonNull String transactionContentAsCsv) {

    // preliminary check to see if Pan field is found
    String[] transactionSplitted = transactionContentAsCsv.split(";", -1);
    if (transactionSplitted.length < PAN_INDEX) {
      log.debug("It's not possible to apply mask to this string: cannot find the pan field!");
      return transactionContentAsCsv;
    }

    transactionSplitted[PAN_INDEX] = maskPan(transactionSplitted[PAN_INDEX]);

    return String.join(";", transactionSplitted);
  }

  private String maskPan(String pan) {
    return pan.replaceAll("(?<=.{6}).(?=.{4})", "*");
  }
}
