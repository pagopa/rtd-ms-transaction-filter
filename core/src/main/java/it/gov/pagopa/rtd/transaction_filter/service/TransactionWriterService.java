package it.gov.pagopa.rtd.transaction_filter.service;

public interface TransactionWriterService {

    void openFileChannel(String filename);

    void write(String filename, String content);

    void closeAll();

    void storeErrorPans(String hpan);

    public Boolean hasErrorHpan(String hpan);

}
