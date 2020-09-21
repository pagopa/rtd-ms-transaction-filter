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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
* Implementation for {@link HpanRestClient}
*/

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

    @Value("${rest-client.hpan.list.dateValidation}")
    private Boolean dateValidation;

    @Value("${rest-client.hpan.list.dateValidationHeaderName}")
    private String dateValidationHeaderName;

    @Value("${rest-client.hpan.list.dateValidationPattern}")
    private String dateValidationPattern;

    private final HpanRestConnector hpanRestConnector;

    private OffsetDateTime validationDate;

    /**
    * Method used for recovering the list, if the properties are enabled, an attempt of validating
     * the checksum recovered from the configured header name, and eventually extracting the .csv or .pgp
     * file from the compressed file obtained through the request
     */
    @SneakyThrows
    @Override
    public File getList() {

        File tempFile = File.createTempFile("hpanDownloadFile", "");
        ResponseEntity<Resource> responseEntity = hpanRestConnector.getList(apiKey);

        if (dateValidation) {
            String dateString = Objects.requireNonNull(responseEntity.getHeaders()
                    .get(dateValidationHeaderName)).get(0);
            DateTimeFormatter dtf = dateValidationPattern != null && !dateValidationPattern.isEmpty() ?
                    DateTimeFormatter.ofPattern(dateValidationPattern).withZone(ZoneId.systemDefault()):
                    DateTimeFormatter.RFC_1123_DATE_TIME;
            OffsetDateTime fileCreationDateTime = ZonedDateTime.parse(dateString, dtf).toOffsetDateTime();
            OffsetDateTime currentDate = validationDate != null ? validationDate : OffsetDateTime.now();
            long differenceHours = ChronoUnit.HOURS.between(fileCreationDateTime, this.validationDate != null?
                    this.validationDate : currentDate);
            boolean sameMinutes = currentDate.getMinute() == fileCreationDateTime.getMinute();
            boolean sameSeconds = currentDate.getSecond() == fileCreationDateTime.getSecond();
            boolean sameMillis = currentDate.get(ChronoField.MILLI_OF_SECOND) ==
                    fileCreationDateTime.get(ChronoField.MILLI_OF_SECOND);

            if (differenceHours > 24 ||
                (differenceHours == 24 && (!sameMillis || !sameSeconds || !sameMinutes))) {
                throw new Exception("Recovered PAN list exceeding a day");
            }
        }

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

    public void setValidationDate(OffsetDateTime now) {
        this.validationDate = now;
    }

}
