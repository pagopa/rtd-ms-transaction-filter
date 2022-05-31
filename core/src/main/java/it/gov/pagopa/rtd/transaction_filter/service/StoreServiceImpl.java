package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.service.store.AccountingDate;
import it.gov.pagopa.rtd.transaction_filter.service.store.AccountingDateFlyweight;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerCode;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerCodeFlyweight;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerId;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerIdFlyweight;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationData;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
* Implementation of {@link StoreService}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreServiceImpl implements StoreService {

    private final TreeSet<String> hpanSet;
    private final HashMap<String, String> keyMap = new HashMap<>();
    private final Map<AggregationKey, AggregationData> aggregates = new HashMap<>();
    private String salt = "";
    private String targetInputFile;
    private String targetInputFileHash;
    private AcquirerCodeFlyweight acquirerCodeFlyweight = new AcquirerCodeFlyweight();
    private AcquirerIdFlyweight acquirerIdFlyweight = new AcquirerIdFlyweight();
    private AccountingDateFlyweight accountingDateFlyweight = new AccountingDateFlyweight();
    private Map<String, String> fakeAbiToFiscalCodeMap = new HashMap<>();

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
    public synchronized boolean storeAggregate(AggregationKey key, int amount, String vat, String posType) {
        aggregates.putIfAbsent(key, new AggregationData());
        AggregationData data = aggregates.get(key);
        data.incNumTrx();
        data.incTotalAmount(amount);
        boolean dirtyVat = data.updateVatOrMarkAsDirty(vat);
        boolean dirtyPosType = data.updatePosTypeOrMarkAsDirty(posType);
        return dirtyVat || dirtyPosType;
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

    public String getTargetInputFileAbiPart() {
        return this.targetInputFile.substring(6, 11);
    }

    @Override
    public void setTargetInputFileHash(String hash) {
        this.targetInputFileHash = hash;
    }

    @Override
    public String getTargetInputFileHash() {
        return this.targetInputFileHash;
    }

    public AcquirerCode flyweightAcquirerCode(String acquirerCode) {
        return this.acquirerCodeFlyweight.createAcquirerCode(acquirerCode);
    }

    public AcquirerCodeFlyweight getAcquirerCodeFlyweight() {
        return this.acquirerCodeFlyweight;
    }

    public AcquirerId flyweightAcquirerId(String acquirerId) {
        return this.acquirerIdFlyweight.createAcquirerId(acquirerId);
    }

    public AccountingDate flyweightAccountingDate(String accountingDate) {
        return this.accountingDateFlyweight.createAccountingDate(accountingDate);
    }

    @Override
    public AcquirerId flyweightAcquirerIdToFiscalCode(String acquirerId) {
        String acquirerFiscalCode = fakeAbiToFiscalCodeMap.getOrDefault(acquirerId, acquirerId);
        return this.acquirerIdFlyweight.createAcquirerId(acquirerFiscalCode);
    }

    @Override
    public void setAbiToFiscalCodeMap(Map<String, String> fakeAbiToFiscalCodeMap) {
        this.fakeAbiToFiscalCodeMap = fakeAbiToFiscalCodeMap;
    }

    @Override
    public void clearAll() {
        hpanSet.clear();
        keyMap.clear();
        aggregates.clear();
        this.salt = "";
        this.targetInputFile = null;
        this.targetInputFileHash = null;
        this.acquirerCodeFlyweight.clean();
        this.accountingDateFlyweight.clean();
        this.acquirerIdFlyweight.clean();
        fakeAbiToFiscalCodeMap.clear();
    }

}
