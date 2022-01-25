package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SasResponse;

import java.io.File;

/**
 *  Inteface for the service to be called for recovering the hpan list and salt from remote endpoints
 * @see HpanConnectorServiceImpl
 */
public interface HpanConnectorService {

    /**
     * Method to be called for recovering the pan list file
     * @return .csv or .pgp file containing the list of pans
     */
    File getHpanList();

    /**
    * Method to be called for recovering the salt to be used for applying the hash
     * @return a String to be used as salt for the eventual sha256 pan
     */
    String getSalt();

    /**
     * TODO
     * @return
     */
    SasResponse getSasToken(HpanRestClient.SasScope scope);

    /**
     * TODO
     */
    Void uploadFile(File fileToUpload, String sas, String authorizedContainer);

    /**
     * Method to clean all temp files
     */
    void cleanAllTempFiles();

}
