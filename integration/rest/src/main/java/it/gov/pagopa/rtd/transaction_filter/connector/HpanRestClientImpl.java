package it.gov.pagopa.rtd.transaction_filter.connector;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@RequiredArgsConstructor
@Slf4j
class HpanRestClientImpl implements HpanRestClient {

    @Value("${rest-client.hpan.base-url}")
    private String baseUrl;

    @Value("${rest-client.hpan.api.key}")
    private String apiKey;

    @Value("${rest-client.hpan.list.url}")
    private String listUrl;

    @Value("${rest-client.hpan.list.checksumValidation}")
    private Boolean checksumValidation;

    @Value("${rest-client.hpan.list.checksumHeaderName}")
    private String checksumHeaderName;

    @Value("${rest-client.hpan.list.attemptExtraction}")
    private Boolean attemptExtraction;

    @Value("${rest-client.hpan.list.listFilePattern}")
    private String listFilePattern;

    private final HpanRestConnector hpanRestConnector;

    @SneakyThrows
    @Override
    public File getList() {

        File tempFile = File.createTempFile("hpanDownloadFile", "");
        ResponseEntity<Resource> responseEntity = hpanRestConnector.getList(apiKey);

        try (FileOutputStream tempFileFOS = new FileOutputStream(tempFile)) {

            if (checksumValidation) {
                String checksum = responseEntity.getHeaders().get(checksumHeaderName).get(0);
                if (!checksum.equals(DigestUtils.sha256Hex(responseEntity.getBody().getInputStream()))) {
                    throw new Exception();
                }
            }

            StreamUtils.copy(responseEntity.getBody().getInputStream(), tempFileFOS);
        }

        if (attemptExtraction) {

            ZipFile zipFile = new ZipFile(tempFile);
            Path tempDirWithPrefix = Files.createTempDirectory("hpanTempFolder");

            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

            while (enumeration.hasMoreElements()) {

                FileOutputStream tempFileFOS = null;
                InputStream zipEntryIS = null;

                try {

                    ZipEntry zipEntry = enumeration.nextElement();
                    zipEntryIS = zipFile.getInputStream(zipEntry);
                    File newFile = new File(
                            tempDirWithPrefix.toFile().getAbsolutePath() +
                                    File.separator + zipEntry.getName());
                    new File(newFile.getParent()).mkdirs();

                    tempFileFOS = new FileOutputStream(newFile);
                    IOUtils.copy(zipEntryIS, tempFileFOS);

                    if (log.isDebugEnabled()) {
                        log.debug("Matching " + zipEntry.getName() + " with " + listFilePattern);
                    }

                    if (zipEntry.getName().matches(listFilePattern)) {
                        tempFile = newFile;
                    }

                } finally {

                    if (zipEntryIS != null) {
                        zipEntryIS.close();
                    }

                    if (tempFileFOS != null) {
                        tempFileFOS.close();
                    }

                }
            }
        }

        return tempFile;
    }

    @Override
    public String getSalt() {
        return hpanRestConnector.getSalt(apiKey);
    }

}
