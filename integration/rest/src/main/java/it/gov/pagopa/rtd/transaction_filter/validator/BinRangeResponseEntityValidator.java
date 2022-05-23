package it.gov.pagopa.rtd.transaction_filter.validator;

import it.gov.pagopa.rtd.transaction_filter.connector.BinRangeResponse;
import javax.validation.ValidationException;
import javax.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Custom validation on objects of type BinRangeResponse
 */
@Component
public class BinRangeResponseEntityValidator extends
    BasicResponseEntityValidator<BinRangeResponse> {

  public static final String NUM_FILES_DOES_NOT_MATCH_LINKS = "NumberOfFiles does not match FileLinks size.";

  public BinRangeResponseEntityValidator(Validator validator) {
    super(validator);
  }

  @Override
  protected void validateBody(BinRangeResponse body) {
    super.validateBody(body);

    if (body.getFileLinks().size() != body.getNumberOfFiles()) {
      throw new ValidationException(NUM_FILES_DOES_NOT_MATCH_LINKS);
    }
  }
}
