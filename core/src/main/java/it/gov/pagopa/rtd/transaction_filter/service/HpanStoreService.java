package it.gov.pagopa.rtd.transaction_filter.service;

public interface HpanStoreService {

    void store(String hpan);

    Boolean hasHpan(String hpan);

    void clearAll();

}
