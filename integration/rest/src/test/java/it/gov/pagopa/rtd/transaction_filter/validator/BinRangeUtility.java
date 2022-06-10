package it.gov.pagopa.rtd.transaction_filter.validator;

import it.gov.pagopa.rtd.transaction_filter.connector.BinRangeResponse;
import java.time.OffsetDateTime;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class BinRangeUtility {

  private BinRangeUtility() {}

  public static BinRangeResponse createValidBinRangeResponse() {
    BinRangeResponse stubResponse = new BinRangeResponse();
    stubResponse.setFileLinks(Arrays.asList("link1", "link2"));
    stubResponse.setGenerationDate(OffsetDateTime.now().minusMonths(2));
    stubResponse.setNumberOfFiles(2);
    stubResponse.setAvailableUntil(OffsetDateTime.now().plusMonths(1));

    return stubResponse;
  }

  public static ResponseEntity<BinRangeResponse> createValidResponseEntity(HttpStatus status) {
    return ResponseEntity.status(status).body(createValidBinRangeResponse());
  }

  public static ResponseEntity<BinRangeResponse> createInvalidResponse() {
    BinRangeResponse invalidResponse = createValidBinRangeResponse();
    invalidResponse.setFileLinks(null);
    return ResponseEntity.ok(invalidResponse);
  }
}
