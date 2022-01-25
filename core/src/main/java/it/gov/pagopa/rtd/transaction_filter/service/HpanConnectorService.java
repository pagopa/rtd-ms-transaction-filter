package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SasResponse;

import java.io.File;

/**
 * Interface for the service to be called for recovering the hpan list and salt from remote endpoints.
 *
 * @see HpanConnectorServiceImpl
 */
public interface HpanConnectorService {

    /**
     * Method to be called for recovering the pan list file.
     *
     * @return .csv or .pgp file containing the list of pans
     */
    File getHpanList();

    /**
    * Method to be called for recovering the salt to be used for applying the hash.
     *
     * @return a String to be used as salt for the eventual sha256 pan
     */
    String getSalt();

    /**
     * Method to be called to obtain a Service SAS to be used for a subsequent upload.
     *
     * @param scope the scope for which the SAS is asked for (i.e. ade or cstar)
     * @return the SAS token details
     */
    SasResponse getSasToken(HpanRestClient.SasScope scope);

    /**
     * Method to be called to upload a transaction to a remote storage exposed via REST.
     *
     * @param fileToUpload the file to upload
     * @param sas the SAS token string
     * @param authorizedContainer the container authorized by the SAS token
     */
    Void uploadFile(File fileToUpload, String sas, String authorizedContainer);

    /**
     * Method to clean all temporary files.
     */
    void cleanAllTempFiles();

}
