package it.gov.pagopa.rtd.transaction_filter.batch.model;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Model for the processed lines in the batch.
 * Documentation: https://app.gitbook.com/o/KXYtsf32WSKm6ga638R3/s/A5nRaBVrAjc1Sj7y0pYS/acquirer-integration-with-pagopa-centrostella/integration/standard-pagopa-file-transactions
 * Example files in /opt_resources/example_files
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"idTrxAcquirer", "acquirerCode", "trxDate"}, callSuper = false)
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
    String acquirerCode;

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
    String pan;

    /**
     * Payment operation type
     */
    @NotNull
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[0-9]{2}")
    String operationType;

    /**
     * Payment circuit type
     */
    @NotNull
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[0-9]{2}")
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
    String correlationId;

    /**
     * Transaction amount
     */
    @NotNull
    Long amount;

    /**
     * Transaction amount currency
     */
    @Size(max = 3)
    String amountCurrency;

    /**
     * Merchant category code where the transaction occurred
     */
    @NotNull
    @NotBlank
    String mcc;

    /**
     * Acquirer identifier for the transaction
     */
    @NotNull
    @NotBlank
    String acquirerId;

    /**
     * Merchant identifier where the transaction occurred
     */
    @NotNull
    @NotBlank
    String merchantId;

    /**
     * Identifier for the terminal where the transaction occurred
     */
    @NotNull
    @NotBlank
    String terminalId;

    /**
     * Bank identification number for the transaction
     */
    @NotNull
    @Pattern(regexp = "([0-9]{6}|[0-9]{8})")
    String bin;

    /**
     * Identifier for the merchant VAT
     */
    @NotNull
    @NotBlank
    @Size(max = 50)
    String vat;

    /**
     * POS type
     */
    @NotNull
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[0-9]{2}")
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