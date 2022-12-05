package it.gov.pagopa.rtd.transaction_filter.connector;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.List;
import javax.validation.ValidationException;
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
@TestPropertySource(
    locations = "classpath:config/rest-client.properties",
    properties = {
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
        "spring.application.name=rtd-ms-transaction-filter-integration-rest"})
@ContextConfiguration(initializers = FileReportRestClientTest.RandomPortInitializer.class, classes = {
    HpanRestConnectorConfig.class, FileReportRestClientImpl.class,
    BasicResponseEntityValidator.class, ValidatorConfig.class, HpanRestConnector.class,
    FeignAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
public class FileReportRestClientTest {

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
      .extensions(new ResponseTemplateTransformer(true))
  );

  @After
  public void cleanup() {
    wireMockRule.resetToDefaultMappings();
  }

  @SneakyThrows
  @Test
  public void whenGetFileReportThenFileNameAndContentMatch() {
    FileReport fileReport = restClient.getFileReport();

    assertThat(fileReport).isNotNull().extracting(FileReport::getFilesRecentlyUploaded).asList().hasSize(2);
    assertThat(fileReport.getFilesRecentlyUploaded()).containsAll(getDefaultReport());
  }

  @SneakyThrows
  @Test
  public void whenGetFileReportWithEmptyBodyThenReportIsEmpty() {
    wireMockRule.stubFor(
        get(urlPathEqualTo("/rtd/file-reporter/file-report"))
            .willReturn(
                aResponse().withBody(mapper.writeValueAsString(getEmptyFileReport()))
                    .withHeader("Content-type", "application/json")));

    FileReport fileReport = restClient.getFileReport();

    assertThat(fileReport).isNotNull().extracting(FileReport::getFilesRecentlyUploaded).asList().isEmpty();
  }

  @SneakyThrows
  @Test
  public void givenStatus404WhenGetFileReportThenThrowException() {
    wireMockRule.stubFor(
        get(urlPathEqualTo("/rtd/file-reporter/file-report"))
            .willReturn(
                aResponse().withStatus(404)
                    .withBody(mapper.writeValueAsString(getEmptyFileReport()))
                    .withHeader("Content-type", "application/json")));

    assertThatThrownBy(() -> restClient.getFileReport()).isInstanceOf(FeignException.class);
  }

  @SneakyThrows
  @Test
  public void givenMalformedBodyWhenGetFileReportThenThrowException() {
    wireMockRule.stubFor(
        get(urlPathEqualTo("/rtd/file-reporter/file-report"))
            .willReturn(
                aResponse().withBody(mapper.writeValueAsString(getMalformedReport()))
                    .withHeader("Content-type", "application/json")));

    assertThatThrownBy(() -> restClient.getFileReport()).isInstanceOf(
        ValidationException.class);
  }

  private List<FileMetadata> getDefaultReport() {
    FileMetadata file1 = new FileMetadata();
    file1.setName("ADE.file1.pgp");
    file1.setSize(200L);
    file1.setStatus("SUCCESS");
    file1.setTransmissionDate(LocalDateTime.of(2022, 10, 30, 10, 0, 0, 123000000));
    FileMetadata file2 = new FileMetadata();
    file2.setName("ADE.file2.pgp");
    file2.setSize(500L);
    file2.setStatus("SUCCESS");
    file2.setTransmissionDate(LocalDateTime.of(2022, 10, 31, 10, 0, 0, 123000000));
    return Lists.list(file1, file2);
  }

  private FileReport getEmptyFileReport() {
    FileReport fileReport = new FileReport();
    fileReport.setFilesRecentlyUploaded(Collections.emptyList());
    return fileReport;
  }

  private FileReport getMalformedReport() {
    FileReport fileReport = new FileReport();
    FileMetadata file = new FileMetadata();
    // missing mandatory fields like filename
    file.setSize(200L);
    fileReport.setFilesRecentlyUploaded(Collections.singleton(file));
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