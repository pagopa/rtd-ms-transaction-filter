package it.gov.pagopa.rtd.transaction_filter.connector;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
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

    @Value("${rest-client.hpan.list.dateValidationZone}")
    private String dateValidationZone;

    @Value("${rest-client.hpan.list.partialFileRecovery}")
    private Boolean partialFileRecovery;

    @Value("${rest-client.hpan.list.nextPartHeader}")
    private String nextPartHeader;

    private final HpanRestConnector hpanRestConnector;

    private LocalDateTime validationDate;

    private final List<File> tempHpanFiles;
    private final List<File> tempParFiles;
    private Path tempHpanDirWithPrefix;

    /**
    * Method used for recovering the list, if the properties are enabled, an attempt of validating
     * the checksum recovered from the configured header name, and eventually extracting the .csv or .pgp
     * file from the compressed file obtained through the request
     */
    @SneakyThrows
    @Override
    public List<File> getHpanList() {
        if (!partialFileRecovery) {
           return fullFileHpanRecovery();
        } else {
            String filePartId = "1";
            return partialFileHpanRecovery(filePartId);
        }
    }

    /**
     * Method used for recovering the list, if the properties are enabled, an attempt of validating
     * the checksum recovered from the configured header name, and eventually extracting the .csv or .pgp
     * file from the compressed file obtained through the request
     */
    @SneakyThrows
    @Override
    public List<File> getParList() {
        if (!partialFileRecovery) {
            return fullFileParRecovery();
        } else {
            String filePartId = "1";
            return partialFileParRecovery(filePartId);
        }
    }

    @SneakyThrows
    private List<File> fullFileParRecovery() {
        File tempParFile = File.createTempFile("parDownloadFile", "");
        tempParFiles.add(tempParFile);
        File localTempFile = tempParFile;
        ResponseEntity<Resource> responseEntity = hpanRestConnector.getParList(apiKey);
        localTempFile = processFile(localTempFile, responseEntity);
        return Collections.singletonList(localTempFile);
    }

    @SneakyThrows
    private List<File> partialFileParRecovery(String filePartId) {
        File tempFile = File.createTempFile("parDownloadFile_"
                .concat(filePartId), "");
        tempParFiles.add(tempFile);
        ResponseEntity<Resource> responseEntity = hpanRestConnector
                .getPartialParList(apiKey, filePartId);
        tempFile = processFile(tempFile, responseEntity);
        List<String> nextPartHeaderValues = responseEntity.getHeaders().get(nextPartHeader);
        String nextPartHeaderValue = null;
        if (nextPartHeaderValues != null) {
            nextPartHeaderValue = nextPartHeaderValues.get(0);
        }

        List<File> returnFile = new ArrayList<>(Collections.singletonList(tempFile));

        if (nextPartHeaderValue != null) {
            returnFile.addAll(partialFileHpanRecovery(nextPartHeaderValue));
        }

        return returnFile;
    }

    @SneakyThrows
    private List<File> fullFileHpanRecovery() {
        File tempHpanFile = File.createTempFile("hpanDownloadFile", "");
        tempHpanFiles.add(tempHpanFile);
        File localTempFile = tempHpanFile;
        ResponseEntity<Resource> responseEntity = hpanRestConnector.getHpanList(apiKey);
        localTempFile = processFile(localTempFile, responseEntity);
        return Collections.singletonList(localTempFile);
    }

    @SneakyThrows
    private List<File> partialFileHpanRecovery(String filePartId) {
        File tempFile = File.createTempFile("hpanDownloadFile_"
                .concat(filePartId), "");
        tempHpanFiles.add(tempFile);
        ResponseEntity<Resource> responseEntity = hpanRestConnector
                .getPartialList(apiKey, filePartId);
        tempFile = processFile(tempFile, responseEntity);
        List<String> nextPartHeaderValues = responseEntity.getHeaders().get(nextPartHeader);
        String nextPartHeaderValue = null;
        if (nextPartHeaderValues != null) {
            nextPartHeaderValue = nextPartHeaderValues.get(0);
        }

        List<File> returnFile = new ArrayList<>(Collections.singletonList(tempFile));

        if (nextPartHeaderValue != null) {
            returnFile.addAll(partialFileHpanRecovery(nextPartHeaderValue));
        }

        return returnFile;
    }

    @SneakyThrows
    private File processFile(File tempFile, ResponseEntity<Resource> responseEntity) {

        if (dateValidation) {
            String dateString = Objects.requireNonNull(responseEntity.getHeaders()
                    .get(dateValidationHeaderName)).get(0);
            DateTimeFormatter dtf = dateValidationPattern != null && !dateValidationPattern.isEmpty() ?
                    DateTimeFormatter.ofPattern(dateValidationPattern).withZone(ZoneId.systemDefault()):
                    DateTimeFormatter.RFC_1123_DATE_TIME;

            ZonedDateTime fileCreationDateTime = LocalDateTime.parse(dateString, dtf)
                    .atZone(ZoneId.of(dateValidationZone));
            ;           ZonedDateTime currentDate = validationDate != null ?
                    validationDate.atZone(ZoneId.of(dateValidationZone)) :
                    LocalDateTime.now().atZone(ZoneId.of(dateValidationZone));

            DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("dd-M-yyyy hh:mm:ss a");

            log.debug("currentDate is: {}", dayFormat.format(currentDate));
            log.debug("fileCreationTime is {}", dayFormat.format(fileCreationDateTime));

            boolean sameYear = ChronoUnit.YEARS.between(fileCreationDateTime,currentDate) == 0;
            boolean sameMonth = ChronoUnit.MONTHS.between(fileCreationDateTime,currentDate) == 0;
            boolean sameDay =  ChronoUnit.DAYS.between(fileCreationDateTime,currentDate) == 0;

            if (!sameYear | !sameMonth | !sameDay) {
                throw new Exception("Recovered PAN list exceeding a day");
            }

        }

        try (FileOutputStream tempFileFOS = new FileOutputStream(tempFile)) {

            if (checksumValidation) {
                String checksum = Objects.requireNonNull(
                        responseEntity.getHeaders().get(checksumHeaderName)).get(0);
                if (!checksum.equals(DigestUtils.sha256Hex(Objects.requireNonNull(
                        responseEntity.getBody()).getInputStream()))) {
                    throw new Exception();
                }
            }

            StreamUtils.copy(Objects.requireNonNull(
                    responseEntity.getBody()).getInputStream(), tempFileFOS);
        }

        if (attemptExtraction) {

            try (ZipFile zipFile = new ZipFile(tempFile)) {

                tempHpanDirWithPrefix = Files.createTempDirectory("hpanTempFolder");

                Enumeration<? extends ZipEntry> enumeration = zipFile.entries();

                while (enumeration.hasMoreElements()) {

                    FileOutputStream tempFileFOS = null;
                    InputStream zipEntryIS = null;

                    try {

                        ZipEntry zipEntry = enumeration.nextElement();
                        zipEntryIS = zipFile.getInputStream(zipEntry);
                        File newFile = new File(
                                tempHpanDirWithPrefix.toFile().getAbsolutePath() +
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
        }

        return tempFile;
    }

    @Override
    public String getSalt() {
        return hpanRestConnector.getSalt(apiKey);
    }

    public void setValidationDate(LocalDateTime now) {
        this.validationDate = now;
    }

    @Override
    public void cleanTempFile() {

        try {
            tempHpanFiles.forEach(tempHpanFile -> {
                try {
                    FileUtils.forceDelete(tempHpanFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            tempHpanFiles.clear();
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }

        try {
            tempParFiles.forEach(tempParFile -> {
                try {
                    FileUtils.forceDelete(tempParFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            tempParFiles.clear();
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }

        try {
            FileUtils.deleteDirectory(tempHpanDirWithPrefix.toFile());
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }

    }

}
