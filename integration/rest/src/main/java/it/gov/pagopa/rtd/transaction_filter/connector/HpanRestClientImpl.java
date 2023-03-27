package it.gov.pagopa.rtd.transaction_filter.connector;

import it.gov.pagopa.rtd.transaction_filter.connector.config.HpanRestConnectorConfig;
import it.gov.pagopa.rtd.transaction_filter.utils.HpanUnzipper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

/**
 * Implementation for {@link HpanRestClient}
 */

@Service
@RequiredArgsConstructor
@Slf4j
class HpanRestClientImpl implements HpanRestClient {

  @Autowired
  ApplicationContext context;

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

  private static final String VALID_FILENAMES_PATTERN = "^[a-z0-9_]+\\.csv$";

  @Value("${rest-client.hpan.storageName}")
  private String storageName;

  @Value("${rest-client.hpan.header.blobType}")
  private String headerXMsBlobType;

  @Value("${rest-client.hpan.header.version}")
  private String headerXMsVersion;

  @Value("${rest-client.hpan.proxy.enabled}")
  private Boolean proxyEnabled;

  @Value("${rest-client.hpan.proxy.host}")
  private String proxyHost;

  @Value("${rest-client.hpan.proxy.port}")
  private Integer proxyPort;

  private final HpanRestConnector hpanRestConnector;
  private final HpanRestConnectorConfig hpanRestConnectorConfig;

  private LocalDateTime validationDate;

  private File tempFile;
  private Path tempDirWithPrefix;

  /**
   * Method used for recovering the list, if the properties are enabled, an attempt of validating
   * the checksum recovered from the configured header name, and eventually extracting the .csv or
   * .pgp file from the compressed file obtained through the request
   */
  @SneakyThrows
  @Override
  public File getList() {

    tempFile = Files.createTempFile("hpanDownloadFile", "").toFile();
    File localTempFile = tempFile;
    ResponseEntity<Resource> responseEntity = hpanRestConnector.getList(apiKey);

    if (Boolean.TRUE.equals(dateValidation)) {
      validateDate(responseEntity.getHeaders().get(dateValidationHeaderName));
    }

    if (Boolean.TRUE.equals(checksumValidation)) {
      validateChecksum(responseEntity);
    }

    copyZippedFileIntoTempFile(responseEntity.getBody());

    if (Boolean.TRUE.equals(attemptExtraction)) {
      localTempFile = extractZipFile(tempFile);
    }

    return localTempFile;
  }

  private void validateDate(List<String> headers) {
    String dateString = Objects.requireNonNull(headers).get(0);
    DateTimeFormatter dtf = dateValidationPattern != null && !dateValidationPattern.isEmpty() ?
        DateTimeFormatter.ofPattern(dateValidationPattern).withZone(ZoneId.systemDefault()) :
        DateTimeFormatter.RFC_1123_DATE_TIME;

    ZonedDateTime fileCreationDateTime = LocalDateTime.parse(dateString, dtf)
        .atZone(ZoneId.of(dateValidationZone));

    ZonedDateTime currentDate = validationDate != null ?
        validationDate.atZone(ZoneId.of(dateValidationZone)) :
        LocalDateTime.now().atZone(ZoneId.of(dateValidationZone));

    DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("dd-M-yyyy hh:mm:ss a");

    log.debug("currentDate is: {}", dayFormat.format(currentDate));
    log.debug("fileCreationTime is {}", dayFormat.format(fileCreationDateTime));

    boolean sameYear = ChronoUnit.YEARS.between(fileCreationDateTime, currentDate) == 0;
    boolean sameMonth = ChronoUnit.MONTHS.between(fileCreationDateTime, currentDate) == 0;
    boolean sameDay = ChronoUnit.DAYS.between(fileCreationDateTime, currentDate) == 0;

    if (!sameYear || !sameMonth || !sameDay) {
      throw new IllegalArgumentException("Recovered PAN list exceeding a day");
    }
  }

  private void validateChecksum(ResponseEntity<Resource> responseEntity) throws IOException {
    String checksum = Objects.requireNonNull(
        responseEntity.getHeaders().get(checksumHeaderName)).get(0);
    if (!checksum.equals(DigestUtils.sha256Hex(Objects.requireNonNull(
        responseEntity.getBody()).getInputStream()))) {
      throw new IllegalArgumentException("Error! Checksum does not match!");
    }
  }

  @SneakyThrows
  private void copyZippedFileIntoTempFile(Resource zipFile) {
    try (FileOutputStream tempFileFOS = new FileOutputStream(tempFile)) {
      StreamUtils.copy(Objects.requireNonNull(zipFile).getInputStream(), tempFileFOS);
    }
  }

  @SneakyThrows
  private File extractZipFile(File tempFile) {

    tempDirWithPrefix = Files.createTempDirectory("hpanTempFolder");

    return HpanUnzipper.builder()
        .fileToUnzip(tempFile)
        .zipThresholdEntries(1000)
        .thresholdSizeUncompressed(20_000_000L * 64)
        .outputDirectory(tempDirWithPrefix)
        .isFilenameValidPredicate(this::isFilenameValidInZipFile)
        .listFilePattern(listFilePattern)
        .build()
        .extractZipFile();
  }

  @Override
  public String getSalt() {
    return hpanRestConnector.getSalt(apiKey);
  }

  @Override
  public SasResponse getSasToken(SasScope scope) {
    if (scope.equals(SasScope.ADE)) {
      return hpanRestConnector.postAdeSas(apiKey, "");
    } else if (scope.equals(SasScope.RTD)) {
      return hpanRestConnector.postRtdSas(apiKey, "");
    } else {
      throw new InvalidParameterException();
    }
  }

  @Override
  public String getPublicKey() {
    return hpanRestConnector.getPublicKey(apiKey);
  }

  @Override
  @SneakyThrows
  public Void uploadFile(File fileToUpload, String sas, String authorizedContainer) {

    List<Header> headers = new ArrayList<>();
    headers.add(new BasicHeader("Ocp-Apim-Subscription-Key", apiKey));
    headers.add(new BasicHeader("User-Agent", hpanRestConnectorConfig.getUserAgent()));
    headers.add(new BasicHeader("x-ms-blob-type", headerXMsBlobType));
    headers.add(new BasicHeader("x-ms-version", headerXMsVersion));

    HpanRestConnectorConfig config = context.getBean(HpanRestConnectorConfig.class);
    SSLContext sslContext = config.getSSLContext();

    HttpClientBuilder httpClientBuilder = HttpClients.custom()
        .setSSLContext(sslContext)
        .setDefaultHeaders(headers);

    if (Boolean.TRUE.equals(proxyEnabled)) {
      httpClientBuilder.setProxy(createProxy());
    }

    HttpClient httpclient = httpClientBuilder.build();

    String uri =
        baseUrl + "/" + storageName + "/" + authorizedContainer + "/" + fileToUpload.getName()
            + "?" + sas;
    final HttpPut httpput = new HttpPut(uri);

    FileEntity entity = new FileEntity(fileToUpload,
        ContentType.create("application/octet-stream"));
    httpput.setEntity(entity);
    final HttpResponse response = httpclient.execute(httpput);
    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
      handleErrorStatus(response.getStatusLine().getStatusCode(), fileToUpload.getName());
    } else {
      log.info("File {} uploaded with success (status was: {})", fileToUpload.getName(),
          response.getStatusLine().getStatusCode());
    }
    return null;
  }

  private HttpHost createProxy() {
    if (proxyHost != null && proxyPort != null) {
      return new HttpHost(proxyHost, proxyPort);
    } else {
      throw new IllegalArgumentException(
          "One or more proxy parameters are null! Please set a value");
    }
  }

  private void handleErrorStatus(int statusCode, String filename) throws IOException {
    if (statusCode == HttpStatus.SC_CONFLICT) {
      log.error(
          "Upload failed for file {} (status was {}: File with same name has already been uploaded)",
          filename, HttpStatus.SC_CONFLICT);
    } else {
      throw new IOException("Upload failed for file " + filename + " (status was: "
          + statusCode + ")");
    }
  }

  public void setValidationDate(LocalDateTime now) {
    this.validationDate = now;
  }

  @Override
  public void cleanTempFile() {

    try {
      FileUtils.forceDelete(tempFile);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    try {
      FileUtils.deleteDirectory(tempDirWithPrefix.toFile());
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

  }

  private boolean isFilenameValidInZipFile(String filename) {
    Pattern pattern = Pattern.compile(VALID_FILENAMES_PATTERN, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(filename);
    return matcher.find();
  }

}
