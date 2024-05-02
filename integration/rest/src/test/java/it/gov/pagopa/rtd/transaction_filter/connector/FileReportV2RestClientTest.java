package it.gov.pagopa.rtd.transaction_filter.connector;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import feign.FeignException;
import it.gov.pagopa.rtd.transaction_filter.connector.config.HpanRestConnectorConfig;
import it.gov.pagopa.rtd.transaction_filter.connector.model.FileMetadata;
import it.gov.pagopa.rtd.transaction_filter.connector.model.FileReport;
import it.gov.pagopa.rtd.transaction_filter.validator.BasicResponseEntityValidator;
import it.gov.pagopa.rtd.transaction_filter.validator.ValidatorConfig;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.ValidationException;
import lombok.SneakyThrows;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.TestPropertySourceUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(locations = "classpath:config/rest-client.properties", properties = {
    "rest-client.hpan.list.url=/rtd/payment-instrument-manager/hashed-pans",
    "rest-client.hpan.salt.url=/rtd/payment-instrument-manager/salt",
    "rest-client.hpan.adesas.url=/rtd/csv-transaction/ade/sas",
    "rest-client.hpan.rtdsas.url=/rtd/csv-transaction/rtd/sas",
    "rest-client.hpan.mtls.enabled=true", "rest-client.hpan.list.checksumHeaderName=checksum",
    "rest-client.hpan.dateValidation.enabled=true",
    "rest-client.hpan.list.dateValidationHeaderName=date",
    "rest-client.hpan.key-store.file=classpath:certs/client-keystore.jks",
    "rest-client.hpan.key-store.password=secret",
    "rest-client.hpan.trust-store.file=classpath:certs/client-truststore.jks",
    "rest-client.hpan.trust-store.password=secret",
    "spring.application.name=rtd-ms-transaction-filter-integration-rest" })
@ContextConfiguration(initializers = FileReportV2RestClientTest.RandomPortInitializer.class, classes = {
    HpanRestConnectorConfig.class, FileReportRestClientImpl.class,
    BasicResponseEntityValidator.class, ValidatorConfig.class, HpanRestConnector.class,
    FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class })
public class FileReportV2RestClientTest {

  @Autowired
  private FileReportRestClientImpl restClient;

  ObjectMapper mapper = new ObjectMapper();

  @ClassRule
  public static WireMockClassRule wireMockRule = new WireMockClassRule(wireMockConfig()
      .dynamicHttpsPort()
      .dynamicPort()
      .needClientAuth(true)
      .keystorePath("src/test/resources/certs/server-keystore.jks")
      .keystorePassword("secret")
      .keyManagerPassword("secret")
      .trustStorePath("src/test/resources/certs/server-truststore.jks")
      .trustStorePassword("secret")
      .usingFilesUnderClasspath("stubs")
      .extensions(new ResponseTemplateTransformer(true)));

  @After
  public void cleanup() {
    wireMockRule.resetToDefaultMappings();
  }

  @SneakyThrows
  @Test
  public void whenGetFileReportV2ThenFileNameAndContentMatch() {
    FileReport fileReport = restClient.getFileReport();

    assertThat(fileReport).isNotNull().extracting(FileReport::getFilesRecentlyUploaded).asList().hasSize(2);

    assertEquals(fileReport.getFilesRecentlyUploaded().get(0).getName(), getDefaultReportV2().get(0).getName());
    assertEquals(fileReport.getFilesRecentlyUploaded().get(0).getSize(), getDefaultReportV2().get(0).getSize());
    assertEquals(fileReport.getFilesRecentlyUploaded().get(0).getStatus(), getDefaultReportV2().get(0).getStatus());
    assertEquals(fileReport.getFilesRecentlyUploaded().get(0).getTransmissionDate(),
        getDefaultReportV2().get(0).getTransmissionDate());

    assertEquals(
        fileReport.getFilesRecentlyUploaded().get(0).getDataSummary().get("countNegativeTransactions"),
        getDefaultReportV2().get(0).getDataSummary().get("countNegativeTransactions"));
    assertEquals(
        fileReport.getFilesRecentlyUploaded().get(0).getDataSummary().get("countPositiveTransactions"),
        getDefaultReportV2().get(0).getDataSummary().get("countPositiveTransactions"));
    assertEquals(fileReport.getFilesRecentlyUploaded().get(0).getDataSummary().get("maxAccountingDate"),
        getDefaultReportV2().get(0).getDataSummary().get("maxAccountingDate"));
    assertEquals(fileReport.getFilesRecentlyUploaded().get(0).getDataSummary().get("minAccountingDate"),
        getDefaultReportV2().get(0).getDataSummary().get("minAccountingDate"));
    assertEquals(fileReport.getFilesRecentlyUploaded().get(0).getDataSummary().get("numberOfMerchants"),
        getDefaultReportV2().get(0).getDataSummary().get("numberOfMerchants"));
    assertEquals(fileReport.getFilesRecentlyUploaded().get(0).getDataSummary().get("sha256OriginFile"),
        getDefaultReportV2().get(0).getDataSummary().get("sha256OriginFile"));
    assertEquals(
        fileReport.getFilesRecentlyUploaded().get(0).getDataSummary().get("sumAmountNegativeTransactions"),
        getDefaultReportV2().get(0).getDataSummary().get("sumAmountNegativeTransactions"));
    assertEquals(
        fileReport.getFilesRecentlyUploaded().get(0).getDataSummary().get("sumAmountPositiveTransactions"),
        getDefaultReportV2().get(0).getDataSummary().get("sumAmountPositiveTransactions"));

    assertEquals(fileReport.getFilesRecentlyUploaded().get(1).getName(), getDefaultReportV2().get(1).getName());
    assertEquals(fileReport.getFilesRecentlyUploaded().get(1).getSize(), getDefaultReportV2().get(1).getSize());
    assertEquals(fileReport.getFilesRecentlyUploaded().get(1).getStatus(), getDefaultReportV2().get(1).getStatus());
    assertEquals(fileReport.getFilesRecentlyUploaded().get(1).getTransmissionDate(),
        getDefaultReportV2().get(1).getTransmissionDate());

    assertEquals(
        fileReport.getFilesRecentlyUploaded().get(1).getDataSummary().get("countNegativeTransactions"),
        getDefaultReportV2().get(1).getDataSummary().get("countNegativeTransactions"));
    assertEquals(
        fileReport.getFilesRecentlyUploaded().get(1).getDataSummary().get("countPositiveTransactions"),
        getDefaultReportV2().get(1).getDataSummary().get("countPositiveTransactions"));
    assertEquals(fileReport.getFilesRecentlyUploaded().get(1).getDataSummary().get("maxAccountingDate"),
        getDefaultReportV2().get(1).getDataSummary().get("maxAccountingDate"));
    assertEquals(fileReport.getFilesRecentlyUploaded().get(1).getDataSummary().get("minAccountingDate"),
        getDefaultReportV2().get(1).getDataSummary().get("minAccountingDate"));
    assertEquals(fileReport.getFilesRecentlyUploaded().get(1).getDataSummary().get("numberOfMerchants"),
        getDefaultReportV2().get(1).getDataSummary().get("numberOfMerchants"));
    assertEquals(fileReport.getFilesRecentlyUploaded().get(1).getDataSummary().get("sha256OriginFile"),
        getDefaultReportV2().get(1).getDataSummary().get("sha256OriginFile"));
    assertEquals(
        fileReport.getFilesRecentlyUploaded().get(1).getDataSummary().get("sumAmountNegativeTransactions"),
        getDefaultReportV2().get(1).getDataSummary().get("sumAmountNegativeTransactions"));
    assertEquals(
        fileReport.getFilesRecentlyUploaded().get(1).getDataSummary().get("sumAmountPositiveTransactions"),
        getDefaultReportV2().get(1).getDataSummary().get("sumAmountPositiveTransactions"));

  }

  @SneakyThrows
  @Test
  public void whenGetFileReportV2WithEmptyBodyThenReportIsEmpty() {
    wireMockRule.stubFor(
        get(urlPathEqualTo("/rtd/file-reporter/v2/file-report"))
            .willReturn(
                aResponse().withBody(mapper.writeValueAsString(getEmptyFileReportV2()))
                    .withHeader("Content-type", "application/json")));

    FileReport fileReport = restClient.getFileReport();

    assertThat(fileReport).isNotNull().extracting(FileReport::getFilesRecentlyUploaded).asList().isEmpty();
  }

  @SneakyThrows
  @Test
  public void givenStatus404WhenGetFileReportV2ThenThrowException() {
    wireMockRule.stubFor(
        get(urlPathEqualTo("/rtd/file-reporter/v2/file-report"))
            .willReturn(
                aResponse().withStatus(404)
                    .withBody(mapper.writeValueAsString(getEmptyFileReportV2()))
                    .withHeader("Content-type", "application/json")));

    assertThatThrownBy(() -> restClient.getFileReport()).isInstanceOf(FeignException.class);
  }

  @SneakyThrows
  @Test
  public void givenMalformedBodyWhenGetFileReportV2ThenThrowException() {
    wireMockRule.stubFor(
        get(urlPathEqualTo("/rtd/file-reporter/v2/file-report"))
            .willReturn(
                aResponse().withBody(mapper.writeValueAsString(getMalformedReportV2()))
                    .withHeader("Content-type", "application/json")));

    assertThatThrownBy(() -> restClient.getFileReport()).isInstanceOf(
        ValidationException.class);
  }

  private List<FileMetadata> getDefaultReportV2() {
    FileMetadata file1 = new FileMetadata();
    Map<String, Object> dataSummary1 = new LinkedHashMap<>();
    file1.setName("ADE.file1.pgp");
    file1.setSize(200L);
    file1.setStatus("SUCCESS");
    file1.setTransmissionDate(LocalDateTime.of(2022, 10, 30, 10, 0, 0, 123000000));
    dataSummary1.put("minAccountingDate", LocalDateTime.of(2022, 10, 28, 10, 0, 0, 123000000).toString());
    dataSummary1.put("maxAccountingDate", LocalDateTime.of(2022, 10, 30, 10, 0, 0, 123000000).toString());
    dataSummary1.put("numberOfMerchants", 123);
    dataSummary1.put("countNegativeTransactions", 283);
    dataSummary1.put("countPositiveTransactions", 980);
    dataSummary1.put("sumAmountNegativeTransactions", 3232323);
    dataSummary1.put("sumAmountPositiveTransactions", 1231232);
    dataSummary1.put("sha256OriginFile",
        "#sha256sum:615bbf196371b6f95b738dccf4a4e3873dff569f7a5c1eb3b50ff52b0718f65d");
    file1.setDataSummary(dataSummary1);

    FileMetadata file2 = new FileMetadata();
    Map<String, Object> dataSummary2 = new LinkedHashMap<>();
    file2.setName("ADE.file2.pgp");
    file2.setSize(500L);
    file2.setStatus("SUCCESS");
    file2.setTransmissionDate(LocalDateTime.of(2022, 10, 31, 10, 0, 0, 123000000));
    dataSummary2.put("minAccountingDate", LocalDateTime.of(2022, 10, 30, 10, 0, 0, 123000000).toString());
    dataSummary2.put("maxAccountingDate", LocalDateTime.of(2022, 10, 31, 10, 0, 0, 123000000).toString());
    dataSummary2.put("numberOfMerchants", 234);
    dataSummary2.put("countNegativeTransactions", 333);
    dataSummary2.put("countPositiveTransactions", 1090);
    dataSummary2.put("sumAmountNegativeTransactions", 890900);
    dataSummary2.put("sumAmountPositiveTransactions", 988898023);
    dataSummary2.put("sha256OriginFile",
        "#sha256sum:615bbf196371b6f95b738dc9823yt3873dff569f7a5c1eb3b50ff52b0718f65d");

    file2.setDataSummary(dataSummary2);

    return Lists.list(file1, file2);
  }

  private FileReport getEmptyFileReportV2() {
    FileReport fileReport = new FileReport();
    fileReport.setFilesRecentlyUploaded(Collections.emptyList());
    return fileReport;
  }

  private FileReport getMalformedReportV2() {
    FileReport fileReport = new FileReport();
    FileMetadata file = new FileMetadata();
    // missing mandatory fields like filename
    file.setSize(200L);
    fileReport.setFilesRecentlyUploaded(Collections.singletonList(file));
    return fileReport;
  }

  public static class RandomPortInitializer implements
      ApplicationContextInitializer<ConfigurableApplicationContext> {

    @SneakyThrows
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
          String.format("rest-client.hpan.base-url=https://localhost:%d/",
              wireMockRule.httpsPort()));
    }
  }
}