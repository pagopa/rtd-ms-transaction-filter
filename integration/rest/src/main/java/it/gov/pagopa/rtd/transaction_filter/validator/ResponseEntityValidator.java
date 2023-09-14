package it.gov.pagopa.rtd.transaction_filter.validator;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

/**
 * Validator interface for custom validation on ResponseEntity objects.
 *
 * @param <T> type of object wrapped by the ResponseEntity
 */
public interface ResponseEntityValidator<T> {

  void validate(ResponseEntity<T> responseEntity);

  void validateStatus(HttpStatusCode status);
}
