package it.gov.pagopa.rtd.transaction_filter.service;

import java.util.concurrent.ExecutorService;

public interface TransactionWriterService {

    void openFileChannel(String filename);

    void write(String filename, String content);

    void closeAll();

}
