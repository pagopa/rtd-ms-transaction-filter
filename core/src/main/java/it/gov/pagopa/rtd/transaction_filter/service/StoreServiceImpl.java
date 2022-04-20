package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationData;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationKey;
import it.gov.pagopa.rtd.transaction_filter.service.store.CurrencyFlyweight;
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
    private final HashMap<String, String> fileHashMap = new HashMap<>();
    private final Map<AggregationKey, AggregationData> aggregates = new HashMap<>();
    private String salt = "";
    private String targetInputFile;

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
    public String getHash(String filename) {
        return this.fileHashMap.get(filename);
    }

    @Override
    public void storeHash(String filename, String hash) {
        this.fileHashMap.put(filename, hash);
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
    public synchronized void storeAggregate(AggregationKey key, int amount, String currency, String vat, String posType) {
        aggregates.putIfAbsent(key, new AggregationData());
        AggregationData data = aggregates.get(key);
        data.incNumTrx();
        data.incTotalAmount(amount);

        if (data.getCurrency() == null) {
            data.setCurrency(CurrencyFlyweight.createCurrency(currency));
        } else {
            if (!data.getCurrency().getIsoCode().equals("###na###") && !data.getCurrency().getIsoCode().equals(currency)) {
                data.setCurrency(CurrencyFlyweight.createCurrency("###na###"));
            }
        }

        data.setVat(vat);

        if (data.getPosType() == 126) {
            if (posType.equals("00")) {
                data.setPosType((byte) 0);
            } else if (posType.equals("01")) {
                data.setPosType((byte) 1);
            }
        } else {
            if (posType.equals("00") && data.getPosType() != 0) {
                data.setPosType((byte) 127);
            }
            if (posType.equals("01") && data.getPosType() != 1) {
                data.setPosType((byte) 127);
            }
        }
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
    public void clearAll() {
        hpanSet.clear();
        keyMap.clear();
        fileHashMap.clear();
        aggregates.clear();
        this.salt = "";
        this.targetInputFile = null;
    }

}
