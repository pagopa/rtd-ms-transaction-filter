package it.gov.pagopa.rtd.transaction_filter.service;

import java.io.File;
import java.util.List;

/**
 *  Inteface for the service to be called for recovering the hpan list and salt from remote endpoints
 * @see HpanConnectorServiceImpl
 */
public interface TokenConnectorService {

    /**
     * Method to be called for recovering the tokenPan list file
     * @return .csv or .pgp file containing the list of token pans
     */
    List<File> getTokenPanList();

    /**
     * Method to be called for recovering the bin list file
     * @return .csv or .pgp file containing the list of bins
     */
    List<File> getBinList();

    /**
     * Method to clean all temp files
     */
    public void cleanAllTempFiles();

}
