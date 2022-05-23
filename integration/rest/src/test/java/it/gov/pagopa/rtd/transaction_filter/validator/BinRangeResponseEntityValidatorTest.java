package it.gov.pagopa.rtd.transaction_filter.validator;

import static it.gov.pagopa.rtd.transaction_filter.validator.BinRangeUtility.createValidResponseEntity;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import it.gov.pagopa.rtd.transaction_filter.connector.BinRangeResponse;
import java.util.Objects;
import javax.validation.ValidationException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class BinRangeResponseEntityValidatorTest {

  ValidatorConfig validatorConfig = new ValidatorConfig();
  private final ResponseEntityValidator<BinRangeResponse> responseEntityValidator =
      new BinRangeResponseEntityValidator(validatorConfig.getValidator());

  @Test
  void whenBinRangeDoesNotMatchLinksAndNumbersOfFilesThenThrowException() {
    ResponseEntity<BinRangeResponse> binRangeResponse = createValidResponseEntity(HttpStatus.OK);
    Objects.requireNonNull(binRangeResponse.getBody()).setNumberOfFiles(3);

    assertThatThrownBy(() -> responseEntityValidator.validate(binRangeResponse))
        .isInstanceOf(ValidationException.class)
        .hasMessage(BinRangeResponseEntityValidator.NUM_FILES_DOES_NOT_MATCH_LINKS);
  }

  @SneakyThrows
  @Test
  void whenBinRangeResponseIsValidThenNoExceptionIsThrown() {
    ResponseEntity<BinRangeResponse> binRangeResponse = createValidResponseEntity(HttpStatus.OK);

    responseEntityValidator.validate(binRangeResponse);
  }
}