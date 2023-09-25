package it.gov.pagopa.rtd.transaction_filter.validator;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definition of default annotation validator.
 */
@Configuration
public class ValidatorConfig {

  @Bean
  public Validator getValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    return factory.getValidator();
  }
}
