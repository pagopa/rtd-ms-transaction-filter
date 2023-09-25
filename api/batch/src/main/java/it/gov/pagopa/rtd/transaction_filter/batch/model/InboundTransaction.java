package it.gov.pagopa.rtd.transaction_filter.batch.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Model for the processed lines in the batch.
 * Example files in /opt_resources/example_files
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"idTrxAcquirer", "senderCode", "trxDate"}, callSuper = false)
public class
InboundTransaction {

    /**
     * Unique Acquirer transaction identifier
     */
    @NotNull
    @NotBlank
    @Size(max = 255)
    String idTrxAcquirer;

    /**
     * Acquirer ABI code
     */
    @NotNull
    @NotBlank
    @Size(max = 20)
    String senderCode;

    /**
     * Date when the transaction occurred
     */
    @NotNull
    @NotBlank
    String trxDate;

    /**
     * Transaction payment instrument PAN
     */
    @NotNull
    @NotBlank
    @Size(max = 64)
    String pan;

    /**
     * Payment operation type
     */
    @NotNull
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "\\d{2}")
    String operationType;

    /**
     * Payment circuit type
     */
    @NotNull
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "\\d{2}")
    String circuitType;

    /**
     * Issuer Authorization identifier code for the transaction
     */
    @NotNull
    @NotBlank
    @Size(max = 255)
    String idTrxIssuer;

    /**
     * Identifier correlating to a previous transaction
     */
    @Size(max = 255)
    String correlationId;

    /**
     * Transaction amount
     * max value: 999.999.999,99€
     */
    @NotNull
    @Min(value = 0)
    @Max(value = 99999999999L)
    Long amount;

    /**
     * Transaction amount currency
     */
    @NotNull
    @NotBlank
    @Size(max = 3)
    @Pattern(regexp = "978")
    String amountCurrency;

    /**
     * Merchant category code where the transaction occurred
     */
    @NotNull
    @NotBlank
    @Size(max = 5)
    String mcc;

    /**
     * Acquirer identifier for the transaction
     */
    @NotNull
    @NotBlank
    @Size(max = 255)
    String acquirerId;

    /**
     * Merchant identifier where the transaction occurred
     */
    @NotNull
    @NotBlank
    @Size(max = 255)
    String merchantId;

    /**
     * Identifier for the terminal where the transaction occurred
     */
    @NotNull
    @NotBlank
    @Size(max = 255)
    String terminalId;

    /**
     * Bank identification number for the transaction
     */
    @NotNull
    @Pattern(regexp = "(\\d{6}|\\d{8})")
    String bin;

    /**
     * Fiscal code
     */
    @NotNull
    @NotBlank
    @Size(max = 50)
    String fiscalCode;

    /**
     * Identifier for the merchant VAT
     */
    @Size(max = 50)
    String vat;

    /**
     * POS type
     */
    @NotNull
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "00|01")
    String posType;

    /**
     * PAR associated with the transaction's PAN, when available
     */
    @Size(max = 255)
    String par;

    /**
     * Internal fields, describing the lineNumber and filename for the record used to extract the other fields
     */
    Integer lineNumber;
    String filename;

}