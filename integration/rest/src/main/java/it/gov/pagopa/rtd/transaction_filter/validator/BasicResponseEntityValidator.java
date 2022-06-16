package it.gov.pagopa.rtd.transaction_filter.validator;


import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
  public void validate(@NotNull ResponseEntity<T> responseEntity) {
    validateStatus(responseEntity.getStatusCode());
    validateHeaders(responseEntity.getHeaders());
    validateBody(responseEntity.getBody());
  }

  @Override
  public void validateStatus(HttpStatus statusCode) {
    if (!statusCode.is2xxSuccessful()) {
      throw new ResponseStatusException(statusCode);
    }
  }

  protected void validateHeaders(HttpHeaders headers) {
    // default empty implementation, override in derived class if needed
  }

  protected void validateBody(T body) {
    Objects.requireNonNull(body);
    Set<ConstraintViolation<T>> violations = validator.validate(body);

    if (!violations.isEmpty()) {
      throw new ValidationException("Body is not valid: " + violations.size() + " errors.");
    }
  }
}
