package it.gov.pagopa.rtd.transaction_filter.batch.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Model for transactions' POS-based aggregates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class
AdeTransactionsAggregate {

    @NotNull
    @NotBlank
    @Size(max = 20)
    String acquirerCode;

    @NotNull
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[0-9]{2}")
    String operationType;

    @NotNull
    @NotBlank
    String transmissionDate;

    @NotNull
    @NotBlank
    String accountingDate;

    @NotNull
    @NotBlank
    int numTrx;

    @NotNull
    @NotBlank
    Long totalAmount;

    @Size(max = 3)
    String currency;

    @NotNull
    @NotBlank
    @Size(max = 255)
    String acquirerId;

    @NotNull
    @NotBlank
    @Size(max = 255)
    String merchantId;

    @NotNull
    @NotBlank
    @Size(max = 255)
    String terminalId;

    @NotNull
    @NotBlank
    @Size(max = 50)
    String fiscalCode;

    @Size(max = 50)
    String vat;

    @NotNull
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[0-9]{2}")
    String posType;

}