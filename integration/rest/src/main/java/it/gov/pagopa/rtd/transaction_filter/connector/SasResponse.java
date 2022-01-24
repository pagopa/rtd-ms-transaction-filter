package it.gov.pagopa.rtd.transaction_filter.connector;

import lombok.Data;

@Data
public class SasResponse {
    private String sas;
    private String authorizedContainer;
}
