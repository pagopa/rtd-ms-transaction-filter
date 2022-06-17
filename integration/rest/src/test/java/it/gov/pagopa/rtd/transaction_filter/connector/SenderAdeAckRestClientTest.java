package it.gov.pagopa.rtd.transaction_filter.connector;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import it.gov.pagopa.rtd.transaction_filter.connector.config.HpanRestConnectorConfig;
import it.gov.pagopa.rtd.transaction_filter.validator.BasicResponseEntityValidator;
import it.gov.pagopa.rtd.transaction_filter.validator.ValidatorConfig;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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
        "rest-client.hpan.mtls.enabled=true",
        "rest-client.hpan.list.checksumHeaderName=checksum",
        "rest-client.hpan.dateValidation.enabled=true",
        "rest-client.hpan.list.dateValidationHeaderName=date",
        "rest-client.hpan.key-store.file=classpath:certs/client-keystore.jks",
        "rest-client.hpan.key-store.password=secret",
        "rest-client.hpan.trust-store.file=classpath:certs/client-truststore.jks",
        "rest-client.hpan.trust-store.password=secret",
        "spring.application.name=rtd-ms-transaction-filter-integration-rest"
    }
)
@ContextConfiguration(initializers = SenderAdeAckRestClientTest.RandomPortInitializer.class,
    classes = {
        HpanRestConnectorConfig.class,
        SenderAdeAckRestClientImpl.class,
        BasicResponseEntityValidator.class,
        ValidatorConfig.class,
        HpanRestConnector.class,
        FeignAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class
    }
)
public class SenderAdeAckRestClientTest {

  @Autowired
  private SenderAdeAckRestClientImpl restClient;

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @ClassRule
  public static WireMockClassRule wireMockRule = new WireMockClassRule(wireMockConfig()
      .dynamicHttpsPort()
      .dynamicPort()
      .needClientAuth(true)
      .keystorePath("src/test/resources/certs/server-keystore.jks")
      .keystorePassword("secret")
      .trustStorePath("src/test/resources/certs/server-truststore.jks")
      .trustStorePassword("secret")
      .usingFilesUnderClasspath("stubs")
      .extensions(new ResponseTemplateTransformer(true))
  );

  @Before
  public void setup() throws IOException {
    restClient.setTempDir(tempDir.getRoot());
  }

  @After
  public void cleanup() {
    wireMockRule.resetAll();
  }

  @SneakyThrows
  @Test
  public void whenDownloadSenderAdeAckFilesThenFileNameAndContentMatch() {
    List<File> files = restClient.getSenderAdeAckFiles();

    assertThat(files).isNotNull().isNotEmpty().hasSize(2);
    assertThat(files.get(0)).hasName("rejectedFiscalCodes1.csv").hasContent("abcd;123456");
    assertThat(files.get(1)).hasName("rejectedFiscalCodes2.csv").hasContent("test;098765");
  }

  @SneakyThrows
  @Test
  public void whenGetSenderAdeAckFilesGivesNoFileNamesThenReturnsEmptyList() {
    wireMockRule.stubFor(get("/rtd/file-register/sender-ade-ack")
        .willReturn(aResponse().withBody("{}").withHeader("Content-type", "application/json")));

    List<File> files = restClient.getSenderAdeAckFiles();

    assertThat(files).isNotNull().isEmpty();
  }

  @SneakyThrows
  @Test
  public void whenDownloadedFileIsEmptyThenDoNotSaveTheFile() {
    wireMockRule.stubFor(get(urlPathMatching("/ade/.*"))
        .willReturn(aResponse().withBodyFile("senderAdeAck/rejectedFiscalCodesEmpty.txt")));

    List<File> files = restClient.getSenderAdeAckFiles();

    assertThat(files).isNotNull().isEmpty();
  }

  @SneakyThrows
  @Test
  public void whenHttpApiReturnsEmptyBodyThenRaisesException() {
    wireMockRule.stubFor(get("/rtd/file-register/sender-ade-ack")
        .willReturn(aResponse()));

    assertThatThrownBy(() -> restClient.getSenderAdeAckFiles()).isInstanceOf(
        NullPointerException.class);
  }

  public static class RandomPortInitializer implements
      ApplicationContextInitializer<ConfigurableApplicationContext> {

    @SneakyThrows
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertySourceUtils
          .addInlinedPropertiesToEnvironment(applicationContext,
              String.format("rest-client.hpan.base-url=https://localhost:%d/",
                  wireMockRule.httpsPort())
          );
    }
  }
}