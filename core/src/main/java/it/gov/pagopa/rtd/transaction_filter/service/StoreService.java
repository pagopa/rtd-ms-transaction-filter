package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.service.store.AccountingDate;
import it.gov.pagopa.rtd.transaction_filter.service.store.SenderCode;
import it.gov.pagopa.rtd.transaction_filter.service.store.SenderCodeFlyweight;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerId;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationData;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationKey;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * Service to be used for storing information shared across steps, e.g.:
 * - the pan list obtained from file
 * - the salt obtained remotely
 * - PGP keys obtained at runtime
 * - Hashes of input files
 *
 * {@link StoreServiceImpl}
 */
public interface StoreService {

    /**
     * Method used to recover the stored salt.
     *
     * @return String containing the salt
     */
    String getSalt();

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
     * @param vat the transaction VAT, if present
     * @param posType the transaction POS type
     * @return boolean true if dirty data has been found during aggregation
     */
    boolean storeAggregate(AggregationKey key, long amount, String vat, String posType);

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
     * Clear aggregates data structure.
     */
    void clearAggregates();

    /**
     * Set the filename of the target input file for current job execution.
     *
     * @param filename the target file name
     */
    void setTargetInputFile(String filename);

    /**
     * Get the filename of the target input file for current job execution.
     *
     * @return a filename string
     */
    String getTargetInputFile();

    /**
     * Get the ABI code embodied in the input filename.
     *
     * @return an ABI code
     */
    String getTargetInputFileAbiPart();

    /**
     * Set the checksum of the target input file for current job execution.
     *
     * @param checksum the target file name
     */
    void setTargetInputFileHash(String checksum);

    /**
     * Get the checksum of the target input file for current job execution.
     *
     * @return a checksum string
     */
    String getTargetInputFileHash();

    /**
     * Instantiate a Sender Code object via Flyweight.
     *
     * @param senderCode a string representing a Sender Code
     * @return a Sender Code object1
     */
    SenderCode flyweightSenderCode(String senderCode);

    /**
     * Return the Flyweight managing Sender Code's instances.
     *
     * @return a Flyweight object
     */
    SenderCodeFlyweight getSenderCodeFlyweight();

    /**
     * Instantiate an Acquirer Id object via Flyweight.
     *
     * @param acquirerId a string representing an Acquirer Id
     * @return an Acquirer Id object
     */
    AcquirerId flyweightAcquirerId(String acquirerId);

    /**
     * Instantiate an Accounting Date object via Flyweight.
     *
     * @param accountingDate a string representing an Accounting Date
     * @return an Accounting Date object
     */
    AccountingDate flyweightAccountingDate(String accountingDate);

    /**
     * Convert the acquirer id to fiscal code (if value is mapped, otherwise use
     * the acquirer id param) and instantiate an AcquirerId object via Flyweight.
     *
     * @param acquirerId a string representing an AcquirerId
     * @return an Acquirer Id object
     */
    AcquirerId flyweightAcquirerIdToFiscalCode(String acquirerId);

    /**
     * Set the map containing the map conversion from acquirerId to fiscal code.
     * Needed for those acquirers who have no abi associated.
     *
     * @param map a conversion map
     */
    void setAbiToFiscalCodeMap(@NotNull Map<String, String> map);

    /**
    * Method explicitly used to clear the stored hpans and salt.
     */
    void clearAll();

}
