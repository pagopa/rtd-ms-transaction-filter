package it.gov.pagopa.rtd.transaction_filter.validator;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
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
