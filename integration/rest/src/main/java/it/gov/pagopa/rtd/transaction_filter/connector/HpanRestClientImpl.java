package it.gov.pagopa.rtd.transaction_filter.connector;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;

@Service
@RequiredArgsConstructor
class HpanRestClientImpl implements HpanRestClient {

    @Value("${rest-client.hpan.base-url}")
    private String baseUrl;

    @Value("${rest-client.hpan.list.url}")
    private String listUrl;

    private final HpanRestConnector hpanRestConnector;

    @SneakyThrows
    @Override
    public File getList() {
        File tempFile = File.createTempFile("hpanDownloadFile", "");
        try (FileOutputStream tempFileFOS = new FileOutputStream(tempFile)) {
            StreamUtils.copy(hpanRestConnector.getList().getInputStream(), tempFileFOS);
        }
        return tempFile;
    }

    @Override
    public String getSalt() {
        return hpanRestConnector.getSalt();
    }

}
