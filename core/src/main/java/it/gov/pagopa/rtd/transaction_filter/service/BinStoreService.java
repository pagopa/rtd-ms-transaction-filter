package it.gov.pagopa.rtd.transaction_filter.service;

/**
* Service to be used for storing the bin list obtained from files, and eventually the salt obtained remotely
* {@link HpanStoreServiceImpl}
*/
public interface BinStoreService {


    void write(String bin);

    /**
     * Method to be used to store an bin
     * @param bin
     */
    void store(String bin);

    /**
     * Method to be called for verifying the presence of an par in the store
     * @param bin
     * @return Boolean defining if the par store contains the input bin
     */
    Boolean hasBin(String bin);

    /**
    * Method explicitly used to clear the stored pars
     */
    void clearAll();

    void clearStoreSet();

    void closeAllWriters();

    void setWorkingBinDirectory(String workingBinDirectory);

    void setNumberPerFile(Long numberPerFile);

    void setCurrentNumberOfData(Long currentNumberOfData);

}
