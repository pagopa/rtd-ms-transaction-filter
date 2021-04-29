package it.gov.pagopa.rtd.transaction_filter.connector;

import java.io.File;
import java.util.List;

/**
* Interface for the REST Client used for recovering the Bin Range list and the TokenPAN list
 */
public interface TokenPanRestClient {

      List<File> getBinList();

      List<File> getTokenList();

      void cleanTempFile();

}
