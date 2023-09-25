package it.gov.pagopa.rtd.transaction_filter.service;

import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SasResponse;
import java.io.File;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

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
    public SasResponse getSasToken(HpanRestClient.SasScope scope) {
        return hpanRestClient.getSasToken(scope);
    }

    @Override
    public String getPublicKey() {
        return hpanRestClient.getPublicKey();
    }

    @Override
    public void uploadFile(File fileToUpload, String sas, String authorizedContainer)
        throws IOException {
        hpanRestClient.uploadFile(fileToUpload, sas, authorizedContainer);
    }

    @Override
    public void cleanAllTempFiles() {
        hpanRestClient.cleanTempFile();
    }

}
