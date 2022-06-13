package it.gov.pagopa.rtd.transaction_filter.validator;

import static it.gov.pagopa.rtd.transaction_filter.validator.ValidatorUtility.createInvalidResponse;
import static it.gov.pagopa.rtd.transaction_filter.validator.ValidatorUtility.createValidResponseEntity;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.validation.ValidationException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class BasicResponseEntityValidatorTest {

  ValidatorConfig validatorConfig = new ValidatorConfig();
  private final ResponseEntityValidator<GenericDto> responseEntityValidator =
      new BasicResponseEntityValidator<>(validatorConfig.getValidator());

  @SneakyThrows
  @Test
  void whenResponseIsValidThenNoExceptionIsThrown() {
    ResponseEntity<GenericDto> response = createValidResponseEntity(HttpStatus.OK);

    responseEntityValidator.validate(response);
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 403, 404, 500})
  void whenHttpStatusIsNot2xxThenThrowException(int httpStatus) {
    ResponseEntity<GenericDto> genericResponse = createValidResponseEntity(
        HttpStatus.resolve(httpStatus));

    assertThatThrownBy(() -> responseEntityValidator.validate(genericResponse))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void whenResponseIsInvalidThenThrowException() {
    ResponseEntity<GenericDto> genericResponse = createInvalidResponse();

    assertThatThrownBy(() -> responseEntityValidator.validate(genericResponse))
        .isInstanceOf(ValidationException.class).hasMessage("Body is not valid.");
  }

  @Test
  void whenResponseEntityHasEmptyBodyThenThrowException() {
    ResponseEntity<GenericDto> genericResponse = ResponseEntity.ok().build();

    assertThatThrownBy(() -> responseEntityValidator.validate(genericResponse))
        .isInstanceOf(NullPointerException.class);
  }
}