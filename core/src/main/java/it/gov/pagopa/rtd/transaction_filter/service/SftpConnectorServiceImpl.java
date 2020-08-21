package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.connector.TransactionSftpConnector;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

/**
* Implementation of {@link SftpConnectorService}
 */

@Service
@RequiredArgsConstructor
class SftpConnectorServiceImpl implements SftpConnectorService {

    private final TransactionSftpConnector transactionSftpConnector;

    @Override
    public void transferFile(File file) {
        transactionSftpConnector.sendFile(file);
    }

}
