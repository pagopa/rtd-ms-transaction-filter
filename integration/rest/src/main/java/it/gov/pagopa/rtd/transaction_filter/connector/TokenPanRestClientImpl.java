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
class TokenPanRestClientImpl implements TokenPanRestClient {

    @Value("${rest-client.tkm.base-url}")
    private String baseUrl;

    @Value("${rest-client.tkm.api.key}")
    private String apiKey;

    @Value("${rest-client.tkm.list.url}")
    private String listUrl;

    @Value("${rest-client.tkm.list.checksumValidation}")
    private Boolean checksumValidation;

    @Value("${rest-client.tkm.list.checksumHeaderName}")
    private String checksumHeaderName;

    @Value("${rest-client.tkm.list.attemptExtraction}")
    private Boolean attemptExtraction;

    @Value("${rest-client.tkm.list.listFilePattern}")
    private String listFilePattern;

    @Value("${rest-client.tkm.list.dateValidation}")
    private Boolean dateValidation;

    @Value("${rest-client.tkm.list.dateValidationHeaderName}")
    private String dateValidationHeaderName;

    @Value("${rest-client.tkm.list.dateValidationPattern}")
    private String dateValidationPattern;

    @Value("${rest-client.tkm.list.dateValidationZone}")
    private String dateValidationZone;

    @Value("${rest-client.tkm.list.partialFileRecovery}")
    private Boolean partialFileRecovery;

    @Value("${rest-client.tkm.list.nextPartHeader}")
    private String nextPartHeader;

    private final TokenPanRestConnector tokenPanRestConnector;

    private LocalDateTime validationDate;

    private final List<File> tempBinFiles;
    private final List<File> tempTokenPanFiles;
    private Path tempHpanDirWithPrefix;

    /**
    * Method used for recovering the list, if the properties are enabled, an attempt of validating
     * the checksum recovered from the configured header name, and eventually extracting the .csv or .pgp
     * file from the compressed file obtained through the request
     */
    @SneakyThrows
    @Override
    public List<File> getBinList() {
        if (!partialFileRecovery) {
           return fullFileBinRecovery();
        } else {
            String filePartId = "1";
            return partialFileBinRecovery(filePartId);
        }
    }

    /**
     * Method used for recovering the list, if the properties are enabled, an attempt of validating
     * the checksum recovered from the configured header name, and eventually extracting the .csv or .pgp
     * file from the compressed file obtained through the request
     */
    @SneakyThrows
    @Override
    public List<File> getTokenList() {
        if (!partialFileRecovery) {
            return fullFileTokenPanRecovery();
        } else {
            String filePartId = "1";
            return partialFileTokenPanRecovery(filePartId);
        }
    }

    @SneakyThrows
    private List<File> fullFileTokenPanRecovery() {
        File tempTokenPanFile = File.createTempFile("tokenPanDownloadFile", "");
        tempTokenPanFiles.add(tempTokenPanFile);
        File localTempFile = tempTokenPanFile;
        ResponseEntity<Resource> responseEntity = tokenPanRestConnector.getTokenList(apiKey);
        localTempFile = processFile(localTempFile, responseEntity);
        return Collections.singletonList(localTempFile);
    }

    @SneakyThrows
    private List<File> partialFileTokenPanRecovery(String filePartId) {
        File tempFile = File.createTempFile("tokenPanDownloadFile_"
                .concat(filePartId), "");
        tempTokenPanFiles.add(tempFile);
        ResponseEntity<Resource> responseEntity = tokenPanRestConnector
                .getPartialTokenList(apiKey, filePartId);
        tempFile = processFile(tempFile, responseEntity);
        List<String> nextPartHeaderValues = responseEntity.getHeaders().get(nextPartHeader);
        String nextPartHeaderValue = null;
        if (nextPartHeaderValues != null) {
            nextPartHeaderValue = nextPartHeaderValues.get(0);
        }

        List<File> returnFile = new ArrayList<>(Collections.singletonList(tempFile));

        if (nextPartHeaderValue != null) {
            returnFile.addAll(partialFileTokenPanRecovery(nextPartHeaderValue));
        }

        return returnFile;
    }

    @SneakyThrows
    private List<File> fullFileBinRecovery() {
        File tempBinFile = File.createTempFile("binDownloadFile", "");
        tempBinFiles.add(tempBinFile);
        File localTempFile = tempBinFile;
        ResponseEntity<Resource> responseEntity = tokenPanRestConnector.getBinList(apiKey);
        localTempFile = processFile(localTempFile, responseEntity);
        return Collections.singletonList(localTempFile);
    }

    @SneakyThrows
    private List<File> partialFileBinRecovery(String filePartId) {
        File tempFile = File.createTempFile("binDownloadFile_"
                .concat(filePartId), "");
        ResponseEntity<Resource> responseEntity = tokenPanRestConnector
                .getBinPartialList(apiKey, filePartId);
        tempFile = processFile(tempFile, responseEntity);
        List<String> nextPartHeaderValues = responseEntity.getHeaders().get(nextPartHeader);
        String nextPartHeaderValue = null;
        if (nextPartHeaderValues != null) {
            nextPartHeaderValue = nextPartHeaderValues.get(0);
        }

        List<File> returnFile = new ArrayList<>(Collections.singletonList(tempFile));

        if (nextPartHeaderValue != null) {
            returnFile.addAll(partialFileBinRecovery(nextPartHeaderValue));
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

    public void setValidationDate(LocalDateTime now) {
        this.validationDate = now;
    }

    @Override
    public void cleanTempFile() {

        try {
            tempTokenPanFiles.forEach(tempTokenPanFile -> {
                try {
                    FileUtils.forceDelete(tempTokenPanFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            tempTokenPanFiles.clear();
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        }

        try {
            tempBinFiles.forEach(tempBinFile -> {
                try {
                    FileUtils.forceDelete(tempBinFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            tempBinFiles.clear();
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
