package it.gov.pagopa.rtd.transaction_filter.connector;

import java.io.File;

/**
* Interface for the REST Client used for recovering the PAN list and the salt
 */
public interface HpanRestClient {

      File getList();

      String getSalt();

      SasResponse getSasToken(String scope);

      Void uploadFile(File fileToUpload, String sas, String authorizedContainer);

      void cleanTempFile();

}
