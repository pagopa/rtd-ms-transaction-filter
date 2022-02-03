package it.gov.pagopa.rtd.transaction_filter.service;

/**
* Service to be used for storing the pan list obtained from files, and eventually the salt obtained remotely
* {@link HpanStoreServiceImpl}
*/
public interface HpanStoreService {

    /**
     * Method to be used to store the salt applied on the pans
     * @param salt salt applied to the pan for the sha256 hashing
     */
    void storeSalt(String salt);

    String getKey(String identifier);

    // TODO
    void storeKey(String identifier, String key);

    /**
    * Method used to recover the stored salt
    * @return String containing the salt
     */
    String getSalt();

    /**
     * Method to be used to store an hpan
     * @param hpan Hashed PAN
     */
    void store(String hpan);

    /**
     * Method to be called for verifying the presence of an hpan in the store
     * @param hpan Hashed PAN
     * @return boolean defining if the hpan store contains the input hpan
     */
    boolean hasHpan(String hpan);

    /**
    * Method explicitly used to clear the stored hpans and salt
     */
    void clearAll();

}
