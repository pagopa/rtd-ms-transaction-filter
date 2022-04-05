package it.gov.pagopa.rtd.transaction_filter.service;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
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
    private final ConcurrentMap<AggregationKey, AggregationData> aggregates = new ConcurrentHashMap<>();
    private String salt = "";

    @Override
    public void storeSalt(String salt) {
        this.salt = salt;
    }

    @Override
    public String getSalt() {
        return this.salt;
    }

    @Override
    public void storeKey(String identifier, String key) {
        this.keyMap.put(identifier, key);
    }

    @Override
    public String getKey(String identifier) {
        return this.keyMap.get(identifier);
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
    public void storeAggregate(AggregationKey key, long amount) {
        aggregates.putIfAbsent(key, new AggregationData());
        aggregates.get(key).getNumTrx().incrementAndGet();
        aggregates.get(key).getTotalAmount().addAndGet(amount);
    }

    @Override
    public AggregationData getAggregate(AggregationKey key) {
        return this.aggregates.get(key);
    }

    public Set<AggregationKey> getAggregateKeySet() {
        return this.aggregates.keySet();
    }

    public Iterator<AggregationKey> aggregateIterator() {
        return this.aggregates.keySet().iterator();
    }

    @Override
    public void clearAll() {
        hpanSet.clear();
        keyMap.clear();
        aggregates.clear();
        this.salt = "";
    }

}
