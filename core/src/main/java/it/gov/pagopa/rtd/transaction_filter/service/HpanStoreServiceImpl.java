package it.gov.pagopa.rtd.transaction_filter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.TreeSet;

/**
* Implementation of {@link HpanStoreService}
 */

// TODO: refactoring - rename the class in a more generic manner e.g. StoreService
@Service
@RequiredArgsConstructor
@Slf4j
class HpanStoreServiceImpl implements HpanStoreService {

    private final TreeSet<String> hpanSet;
    private final HashMap<String, String> keyMap = new HashMap<>();
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
    public void clearAll() {
        hpanSet.clear();
        keyMap.clear();
        this.salt = "";
    }

}
