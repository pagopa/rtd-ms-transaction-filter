package it.gov.pagopa.rtd.transaction_filter.batch.model;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Model for the processed lines in the batch
 */
// FIXME: it's very important to add a comment to each field of the model,
//        that describe its meaning
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"idTrxAcquirer", "acquirerCode", "trxDate"}, callSuper = false)
public class InboundTransaction {

    @NotNull
    @NotBlank
    String idTrxAcquirer;

    @NotNull
    @NotBlank
    @Size(max = 20)
    String acquirerCode;

    @NotNull
    @NotBlank
    String trxDate;

    @NotNull
    @NotBlank
    String pan;

    @NotNull
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[0-9]{2}")
    String operationType;

    @NotNull
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[0-9]{2}")
    String circuitType;

    @NotNull
    @NotBlank
    String idTrxIssuer;

    String correlationId;

    @NotNull
    BigDecimal amount;

    @Size(max = 3)
    String amountCurrency;

    @NotNull
    @NotBlank
    String mcc;

    @NotNull
    @NotBlank
    String acquirerId;

    @NotNull
    @NotBlank
    String merchantId;

    @NotNull
    @NotBlank
    String terminalId;

    @NotNull
    @Pattern(regexp = "([0-9]{6}|[0-9]{8})")
    String bin;

    Integer lineNumber;
    String filename;

}