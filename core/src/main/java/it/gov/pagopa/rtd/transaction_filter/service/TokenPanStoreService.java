package it.gov.pagopa.rtd.transaction_filter.service;

/**
* Service to be used for storing the pan list obtained from files, and eventually the salt obtained remotely
* {@link TokenPanStoreServiceImpl}
*/
public interface TokenPanStoreService {


    void write(String tokenPan);


    /**
     * Method to be used to store an tokenPan
     * @param tokenPan
     */
    void store(String tokenPan);

    /**
     * Method to be called for verifying the presence of an tokenPan in the store
     * @param tokenPan
     * @return Boolean defining if the tokenPan store contains the input tokenPan
     */
    Boolean hasTokenPAN(String tokenPan);

    /**
    * Method explicitly used to clear the stored hpans and salt
     */
    void clearAll();

    void clearStoreSet();

    void closeAllWriters();

    void setWorkingTokenPANDirectory(String workingTokenPANDirectory);

    void setNumberPerFile(Long numberPerFile);

    void setCurrentNumberOfData(Long currentNumberOfData);

}
