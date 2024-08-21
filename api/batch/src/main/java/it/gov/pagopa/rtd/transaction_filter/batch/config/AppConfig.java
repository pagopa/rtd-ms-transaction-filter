package it.gov.pagopa.rtd.transaction_filter.batch.config;

import it.gov.pagopa.rtd.transaction_filter.batch.PathResolver;
import it.gov.pagopa.rtd.transaction_filter.batch.utils.TransactionMaskPolicy;
import it.gov.pagopa.rtd.transaction_filter.batch.utils.TransactionMaskPolicyImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
public class AppConfig {

  @Bean
  public TransactionMaskPolicy getTransactionMaskPolicy() {
    return new TransactionMaskPolicyImpl();
  }

  @Bean
  public PathMatchingResourcePatternResolver resolver() {
    return new PathMatchingResourcePatternResolver();
  }

  @Bean
  public PathResolver pathResolver(PathMatchingResourcePatternResolver resolver) {
    return new PathResolver(resolver);
  }

}
