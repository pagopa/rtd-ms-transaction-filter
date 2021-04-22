package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
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
public class HpanConnectorServiceImpl implements HpanConnectorService {

    private final HpanRestClient hpanRestClient;

    @SneakyThrows
    @Override
    public List<File> getHpanList() {
        return hpanRestClient.getHpanList();
    }

    @SneakyThrows
    @Override
    public List<File> getParList() {
        return hpanRestClient.getParList();
    }

    @Override
    public String getSalt() {
        return hpanRestClient.getSalt();
    }

    @Override
    public void cleanAllTempFiles() {
        hpanRestClient.cleanTempFile();
    }

}
