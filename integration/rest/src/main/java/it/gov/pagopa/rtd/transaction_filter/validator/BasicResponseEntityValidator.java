package it.gov.pagopa.rtd.transaction_filter.validator;


import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Basic implementation of interface ResponseEntityValidator for minimal validation on http status
 * and body annotations.
 */
@Component
@RequiredArgsConstructor
public class BasicResponseEntityValidator<T> implements ResponseEntityValidator<T> {

  private final Validator validator;

  @SneakyThrows
  @Override
  public void validate(ResponseEntity<T> responseEntity) {
    if (!responseEntity.getStatusCode().is2xxSuccessful()) {
      throw new ResponseStatusException(responseEntity.getStatusCode());
    }
    T body = responseEntity.getBody();
    Objects.requireNonNull(body);

    validateBody(body);
  }

  protected void validateBody(T body) {
    Set<ConstraintViolation<T>> violations = validator.validate(body);

    if (!violations.isEmpty()) {
      throw new ValidationException("Body is not valid.");
    }
  }
}
