package it.gov.pagopa.rtd.transaction_filter.service;

/**
* Service to be used for storing the pan list obtained from files, and eventually the salt obtained remotely
* {@link HpanStoreServiceImpl}
*/
public interface ParStoreService {


    void write(String par);

    /**
     * Method to be used to store an par
     * @param par
     */
    void store(String par);

    /**
     * Method to be called for verifying the presence of an par in the store
     * @param par
     * @return Boolean defining if the par store contains the input par
     */
    Boolean hasPar(String par);

    /**
    * Method explicitly used to clear the stored pars
     */
    void clearAll();

    void clearStoreSet();

    void closeAllWriters();

    void setWorkingParDirectory(String workingParDirectory);

    void setNumberPerFile(Long numberPerFile);

    void setCurrentNumberOfData(Long currentNumberOfData);

}
