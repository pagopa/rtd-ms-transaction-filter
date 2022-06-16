package it.gov.pagopa.rtd.transaction_filter.connector;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Interface for the REST Client used for recovering the sender ade ack files (containing the
 * rejected fiscal codes)
 */
public interface SenderAdeAckRestClient {

  /**
   * Retrieve the files containing the ade ack for a specific acquirer.
   * @return a list of files
   * @throws IOException if any problems occur in reading or copying the files
   */
  List<File> getSenderAdeAckFiles() throws IOException;
}
