package it.gov.pagopa.rtd.transaction_filter.batch.utils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import org.junit.jupiter.api.Test;

class TransactionMaskPolicyTest {

  private final TransactionMaskPolicyImpl maskPolicy = new TransactionMaskPolicyImpl();

  @Test
  void whenPanIsWellFormedThenMaskAllButFirst6AndLast4() {
    InboundTransaction transaction = InboundTransaction.builder().pan("1234567890123456").build();

    maskPolicy.apply(transaction);

    assertThat(transaction.getPan()).isEqualTo("123456******3456");
  }

  @Test
  void whenPanIShorterThan10CharThenDoNotMaskIt() {
    InboundTransaction transaction = InboundTransaction.builder().pan("1234567890").build();

    maskPolicy.apply(transaction);

    assertThat(transaction.getPan()).isEqualTo("1234567890");
  }

  @Test
  void givenTransactionAsStringWhenApplyMaskThenMaskIsCorrect() {
    String transactionAsCsv = "99999;00;00;1234567890123456;other;not;important;fields;";

    String transactionMasked = maskPolicy.apply(transactionAsCsv);

    assertThat(transactionMasked).isEqualTo(
        "99999;00;00;123456******3456;other;not;important;fields;");
  }

  @Test
  void givenMalformedTransactionAsStringWhenApplyMaskThenMaskIsNotApplied() {
    String transactionAsCsv = "99999,00,00,1234567890123456";

    String transactionMasked = maskPolicy.apply(transactionAsCsv);

    assertThat(transactionMasked).isEqualTo("99999,00,00,1234567890123456");
  }

}