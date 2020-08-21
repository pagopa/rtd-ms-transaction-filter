package it.gov.pagopa.rtd.transaction_filter.service;

import java.io.File;

/**
 *  Inteface for the service to be called for recovering the hpan list and salt from remote endpoints
 * @see SftpConnectorServiceImpl
 */
public interface SftpConnectorService {

    /**
     * Method to be called for transfering a file through an SFTP channel
     * @param file Output transaction file, to be transfered through SFTP
     */
    void transferFile(File file);

}
