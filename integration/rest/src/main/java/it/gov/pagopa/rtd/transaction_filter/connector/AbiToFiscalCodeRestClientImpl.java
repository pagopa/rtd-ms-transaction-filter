package it.gov.pagopa.rtd.transaction_filter.connector;

import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
* Interface for the REST Client used for recovering the conversion map between fake ABI and FiscalCode
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AbiToFiscalCodeRestClientImpl implements AbiToFiscalCodeRestClient {

      @Value("${rest-client.hpan.api.key}")
      private String apiKey;

      private final HpanRestConnector hpanRestConnector;

      @Override
      public Map<String, String> getFakeAbiToFiscalCodeMap() {
            return Objects.requireNonNull(hpanRestConnector.getFakeAbiToFiscalCodeMap(apiKey));
      }
}
