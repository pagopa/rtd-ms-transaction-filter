package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SasResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import java.io.File;

/**
* Implementation of {@link HpanConnectorService}
 */

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

    @Override
    public SasResponse getSasToken(String scope) {
        return hpanRestClient.getSasToken(scope);
    }

    @Override
    public Void uploadFile(File fileToUpload, String sas, String authorizedContainer) {
        return hpanRestClient.uploadFile(fileToUpload, sas, authorizedContainer);
    }


    @Override
    public void cleanAllTempFiles() {
        hpanRestClient.cleanTempFile();
    }

}
