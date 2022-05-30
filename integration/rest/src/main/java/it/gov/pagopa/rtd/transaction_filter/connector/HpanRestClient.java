package it.gov.pagopa.rtd.transaction_filter.connector;

import java.io.File;
import java.util.Map;

/**
* Interface for the REST Client used for recovering the PAN list and the salt
 */
public interface HpanRestClient {

      enum SasScope { ADE, RTD }

      File getList();

      String getSalt();

      SasResponse getSasToken(SasScope scope);

      String getPublicKey();

      Void uploadFile(File fileToUpload, String sas, String authorizedContainer);

      Map<String, String> getFakeAbiToFiscalCodeMap();

      void cleanTempFile();

}
