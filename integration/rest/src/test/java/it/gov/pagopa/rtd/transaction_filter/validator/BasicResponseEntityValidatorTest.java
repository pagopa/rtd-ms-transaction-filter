package it.gov.pagopa.rtd.transaction_filter.validator;

import static it.gov.pagopa.rtd.transaction_filter.validator.BinRangeUtility.createInvalidResponse;
import static it.gov.pagopa.rtd.transaction_filter.validator.BinRangeUtility.createValidResponseEntity;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.gov.pagopa.rtd.transaction_filter.connector.BinRangeResponse;
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
  private final ResponseEntityValidator<BinRangeResponse> responseEntityValidator =
      new BasicResponseEntityValidator<>(validatorConfig.getValidator());

  @SneakyThrows
  @Test
  void whenResponseIsValidThenNoExceptionIsThrown() {
    ResponseEntity<BinRangeResponse> binRangeResponse = createValidResponseEntity(HttpStatus.OK);

    responseEntityValidator.validate(binRangeResponse);
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 403, 404, 500})
  void whenHttpStatusIsNot2xxThenThrowException(int httpStatus) {
    ResponseEntity<BinRangeResponse> binRangeResponse = createValidResponseEntity(
        HttpStatus.resolve(httpStatus));

    assertThatThrownBy(() -> responseEntityValidator.validate(binRangeResponse))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void whenResponseIsInvalidThenThrowException() {
    ResponseEntity<BinRangeResponse> binRangeResponse = createInvalidResponse();

    assertThatThrownBy(() -> responseEntityValidator.validate(binRangeResponse))
        .isInstanceOf(ValidationException.class).hasMessage("Body is not valid.");
  }

  @Test
  void whenResponseEntityHasEmptyBodyThenThrowException() {
    ResponseEntity<BinRangeResponse> binRangeResponse = ResponseEntity.ok().build();

    assertThatThrownBy(() -> responseEntityValidator.validate(binRangeResponse))
        .isInstanceOf(NullPointerException.class);
  }
}