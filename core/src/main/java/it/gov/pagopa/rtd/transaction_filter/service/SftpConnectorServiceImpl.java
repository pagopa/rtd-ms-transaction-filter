package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.connector.TransactionSftpConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
class SftpConnectorServiceImpl implements SftpConnectorService {

    @Autowired
    TransactionSftpConnector transactionSftpConnector;

    @Override
    public void transferFile(File file) {
        transactionSftpConnector.sendFile(file);
    }

}
