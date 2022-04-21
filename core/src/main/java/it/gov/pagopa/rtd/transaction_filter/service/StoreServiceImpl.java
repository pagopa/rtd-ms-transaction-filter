package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationData;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationKey;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.TreeSet;

/**
* Implementation of {@link StoreService}
 */
@Service
@RequiredArgsConstructor
@Slf4j
class StoreServiceImpl implements StoreService {

    private final TreeSet<String> hpanSet;
    private final HashMap<String, String> keyMap = new HashMap<>();
    private final Map<AggregationKey, AggregationData> aggregates = new HashMap<>();
    private String salt = "";
    private String targetInputFile;
    private String targetInputFileHash;

    @Override
    public String getSalt() {
        return this.salt;
    }

    @Override
    public void storeSalt(String salt) {
        this.salt = salt;
    }

    @Override
    public String getKey(String identifier) {
        return this.keyMap.get(identifier);
    }

    @Override
    public void storeKey(String identifier, String key) {
        this.keyMap.put(identifier, key);
    }

    @Override
    public synchronized void store(String hpan) {
        hpanSet.add(hpan);
    }

    @Override
    public boolean hasHpan(String hpan) {
        return hpanSet.contains(hpan);
    }

    @Override
    public synchronized void storeAggregate(AggregationKey key, int amount, String vat, String posType) {
        aggregates.putIfAbsent(key, new AggregationData());
        AggregationData data = aggregates.get(key);
        data.incNumTrx();
        data.incTotalAmount(amount);
        data.updateVatOrMarkAsDirty(vat);
        data.updatePosTypeOrMarkAsDirty(posType);
    }

    @Override
    public AggregationData getAggregate(AggregationKey key) {
        return this.aggregates.get(key);
    }

    @Override
    public Set<AggregationKey> getAggregateKeySet() {
        return this.aggregates.keySet();
    }

    @Override
    public void clearAggregates() {
        this.aggregates.clear();
    }

    public void setTargetInputFile(String filename) {
        this.targetInputFile = filename;
    }

    public String getTargetInputFile() {
        return this.targetInputFile;
    }

    @Override
    public void setTargetInputFileHash(String hash) {
        this.targetInputFileHash = hash;
    }

    @Override
    public String getTargetInputFileHash() {
        return this.targetInputFileHash;
    }

    @Override
    public void clearAll() {
        hpanSet.clear();
        keyMap.clear();
        aggregates.clear();
        this.salt = "";
        this.targetInputFile = null;
        this.targetInputFileHash = null;
    }

}
