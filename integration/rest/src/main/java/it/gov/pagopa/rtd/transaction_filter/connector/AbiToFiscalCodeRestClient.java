package it.gov.pagopa.rtd.transaction_filter.connector;

import java.util.Map;

/**
 * Interface for the REST Client used for recovering the conversion map between fake ABI and
 * FiscalCode
 */
public interface AbiToFiscalCodeRestClient {

  Map<String, String> getFakeAbiToFiscalCodeMap();
}
