package it.gov.pagopa.rtd.transaction_filter.connector;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import it.gov.pagopa.rtd.transaction_filter.connector.config.HpanRestConnectorConfig;
import java.util.Map;
import lombok.SneakyThrows;
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
@ContextConfiguration(initializers = AbiToFiscalCodeRestClientTest.RandomPortInitializer.class,
    classes = {
        HpanRestConnectorConfig.class,
        AbiToFiscalCodeRestClientImpl.class,
        HpanRestConnector.class,
        FeignAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class
    }
)
public class AbiToFiscalCodeRestClientTest {

  @Autowired
  private AbiToFiscalCodeRestClient restClient;

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
  );

  @Test
  public void getFakeAbiToFiscalCodeMap() {
    Map<String, String> abiToFiscalCodeMap = restClient.getFakeAbiToFiscalCodeMap();

    assertThat(abiToFiscalCodeMap).isNotNull();
    assertThat(abiToFiscalCodeMap.keySet()).hasSize(2);
  }

  @Test
  public void whenGetFakeAbiToFiscalCodeMapReturnsEmptyBodyThenMapIsEmpty() {
    wireMockRule.stubFor(get(urlPathEqualTo("/rtd/abi-to-fiscalcode/conversion-map"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{}"))
    );
    Map<String, String> abiToFiscalCodeMap = restClient.getFakeAbiToFiscalCodeMap();

    assertThat(abiToFiscalCodeMap).isNotNull();
    assertThat(abiToFiscalCodeMap.keySet()).isEmpty();
  }

  public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
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