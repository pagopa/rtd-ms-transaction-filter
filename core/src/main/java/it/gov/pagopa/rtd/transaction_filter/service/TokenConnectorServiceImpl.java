package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.connector.TokenPanRestClient;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/**
* Implementation of {@link HpanConnectorService}
 */

@Service
@RequiredArgsConstructor
public class TokenConnectorServiceImpl implements TokenConnectorService {

    private final TokenPanRestClient tokenPanRestClient;

    @SneakyThrows
    @Override
    public List<File> getTokenPanList() {
        return tokenPanRestClient.getTokenList();
    }

    @SneakyThrows
    @Override
    public List<File> getBinList() {
        return tokenPanRestClient.getBinList();
    }

    @Override
    public void cleanAllTempFiles() {
        tokenPanRestClient.cleanTempFile();
    }

}
