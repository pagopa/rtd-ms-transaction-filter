package it.gov.pagopa.rtd.transaction_filter.batch.config;

import it.gov.pagopa.rtd.transaction_filter.batch.utils.TransactionMaskPolicy;
import it.gov.pagopa.rtd.transaction_filter.batch.utils.TransactionMaskPolicyImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  @Bean
  public TransactionMaskPolicy getTransactionMaskPolicy() {
    return new TransactionMaskPolicyImpl();
  }

}
