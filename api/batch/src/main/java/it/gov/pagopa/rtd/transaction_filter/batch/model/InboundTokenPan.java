package it.gov.pagopa.rtd.transaction_filter.batch.model;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"tokenPan"}, callSuper = false)
public class
InboundTokenPan {

    /** Unique Acquirer transaction identifier */
    @NotNull
    @NotBlank
    String tokenPan;

    /** Payment circuit type */
    @NotNull
    @NotBlank
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[0-9]{2}")
    String circuitType;

    String par;

    /** Internal fields, describing the lineNumber and filename for the record used to extract the other fields */
    Integer lineNumber;
    String filename;

    Boolean valid;

}