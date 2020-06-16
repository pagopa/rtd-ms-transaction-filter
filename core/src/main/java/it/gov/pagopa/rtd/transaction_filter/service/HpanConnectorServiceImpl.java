package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
public class HpanConnectorServiceImpl implements HpanConnectorService {

    private final HpanRestClient hpanRestClient;

    @SneakyThrows
    @Override
    public File getHpanList() {
        return hpanRestClient.getList();
    }

    @Override
    public String getSalt() {
        return hpanRestClient.getSalt();
    }

}
