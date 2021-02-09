package it.gov.pagopa.rtd.transaction_filter.service;

import java.io.IOException;
import java.nio.file.Path;

/**
* Service to be used for storing the pan list obtained from files, and eventually the salt obtained remotely
* {@link HpanStoreServiceImpl}
*/
public interface HpanStoreService {


    void write(String hpan);

    /**
     * Method to be used to store the salt applied on the pans
     * @param salt salt applied to the pan for the sha256 hashing
     */
    void storeSalt(String salt);

    /**
    * Method used to recover the stored salt
    * @returns String containing the salt
     */
    String getSalt();

    /**
     * Method to be used to store an hpan
     * @param hpan
     */
    void store(String hpan);

    /**
     * Method to be called for verifying the presence of an hpan in the store
     * @param hpan
     * @return Boolean defining if the hpan store contains the input hpan
     */
    Boolean hasHpan(String hpan);

    /**
    * Method explicitly used to clear the stored hpans and salt
     */
    void clearAll();

    void closeAllWriters();

    void setWorkingHpanDirectory(String workingHpanDirectory);

    void setNumberPerFile(Long numberPerFile);

    void setCurrentNumberOfData(Long currentNumberOfData);

}
