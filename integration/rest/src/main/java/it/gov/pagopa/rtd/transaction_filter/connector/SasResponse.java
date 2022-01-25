package it.gov.pagopa.rtd.transaction_filter.connector;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
public class SasResponse {

    @Getter
    @Setter
    private String sas;

    @Getter
    @Setter
    private String authorizedContainer;
}
