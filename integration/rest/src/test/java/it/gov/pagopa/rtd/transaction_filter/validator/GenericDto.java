package it.gov.pagopa.rtd.transaction_filter.validator;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenericDto {

  @NotNull
  private String name;
  @NotNull
  private String surname;
}
