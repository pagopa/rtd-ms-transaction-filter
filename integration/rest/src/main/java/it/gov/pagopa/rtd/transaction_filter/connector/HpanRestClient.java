package it.gov.pagopa.rtd.transaction_filter.connector;

import java.io.File;
import java.util.List;

/**
* Interface for the REST Client used for recovering the PAN list and the salt
 */
public interface HpanRestClient {

      List<File> getHpanList();

      List<File> getParList();

      String getSalt();

      void cleanTempFile();

}
