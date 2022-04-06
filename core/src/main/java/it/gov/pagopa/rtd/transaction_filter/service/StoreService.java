package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationData;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationKey;
import java.util.Set;

/**
 * Service to be used for storing information shared across steps, e.g.:
 * - the pan list obtained from file
 * - the salt obtained remotely
 * - PGP keys obtained at runtime
 *
 * {@link StoreServiceImpl}
 */
public interface StoreService {

    /**
     * Method to be used to store the salt applied on the pans.
     *
     * @param salt salt applied to the pan for the sha256 hashing
     */
    void storeSalt(String salt);

    /**
     * Get a previously stored PGP key.
     *
     * @param identifier the key identifier
     * @return a String containing the PGP key
     */
    String getKey(String identifier);

    /**
     * Stores a PGP key.
     *
     * @param identifier the identifier of the key
     * @param key a String containing the PGP key
     */
    void storeKey(String identifier, String key);

    /**
    * Method used to recover the stored salt.
     *
    * @return String containing the salt
     */
    String getSalt();

    /**
     * Method to be used to store a hpan.
     *
     * @param hpan Hashed PAN
     */
    void store(String hpan);

    /**
     * Method to be called for verifying the presence of a hpan in the store.
     *
     * @param hpan Hashed PAN
     * @return boolean defining if the hpan store contains the input hpan
     */
    boolean hasHpan(String hpan);

    /**
     * Aggregates transaction data by a defined group clause (the AggregationKey).
     *
     * @param key the aggregation key
     * @param amount the transaction amount
     * @param currency the transaction currency
     * @param vat the transaction VAT, if present
     * @param posType the transaction POS type
     */
    void storeAggregate(AggregationKey key, long amount, String currency, String vat, String posType);

    /**
     * Get the aggregate computed over a single aggregation key.
     *
     * @param key an aggregation key
     * @return the aggregated data
     */
    AggregationData getAggregate(AggregationKey key);

    /**
     * Get the set of aggregation keys.
     *
     * @return a set of aggregation keys
     */
    Set<AggregationKey> getAggregateKeySet();

    /**
    * Method explicitly used to clear the stored hpans and salt.
     */
    void clearAll();

}
