package it.gov.pagopa.rtd.transaction_filter.connector;

import it.gov.pagopa.rtd.transaction_filter.connector.config.TransactionSftpChannelConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class TransactionSftpConnector {

    private final TransactionSftpChannelConfig.TransactionSftpGateway transactionSftpGateway;

    public void sendFile(File file) {
        transactionSftpGateway.sendToSftp(file);
    }

}
