package it.gov.pagopa.rtd.transaction_filter.validator;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ValidatorUtility {

  private ValidatorUtility() {
  }

  public static GenericDto createValidResponse() {
    GenericDto stub = new GenericDto();
    stub.setName("testname");
    stub.setSurname("testSurname");

    return stub;
  }

  public static ResponseEntity<GenericDto> createValidResponseEntity(HttpStatus status) {
    return ResponseEntity.status(status).body(createValidResponse());
  }

  public static ResponseEntity<GenericDto> createInvalidResponse() {
    GenericDto invalidResponse = createValidResponse();
    invalidResponse.setName(null);
    return ResponseEntity.ok(invalidResponse);
  }
}
