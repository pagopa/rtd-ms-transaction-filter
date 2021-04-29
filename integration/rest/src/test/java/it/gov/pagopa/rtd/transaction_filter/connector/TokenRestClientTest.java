package it.gov.pagopa.rtd.transaction_filter.connector;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import it.gov.pagopa.rtd.transaction_filter.connector.config.TkmRestConnectorConfig;
import lombok.SneakyThrows;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
        locations = "classpath:config/rest-tkm-client.properties",
        properties = {
                "rest-client.bin.list.url=/parlist",
                "rest-client.token.list.url=/list",
                "rest-client.tkm.mtls.enabled=true",
                "rest-client.tkm.list.checksumHeaderName=checksum",
                "rest-client.tkm.dateValidation.enabled=true",
                "rest-client.tkm.list.dateValidationHeaderName=date",
                "rest-client.tkm.key-store.file=classpath:certs/client-keystore.jks",
                "rest-client.tkm.key-store.password=secret",
                "rest-client.tkm.trust-store.file=classpath:certs/client-truststore.jks",
                "rest-client.tkm.trust-store.password=secret",
                "spring.application.name=rtd-ms-transaction-filter-token-integration-rest"
        }
)
@ContextConfiguration(initializers = TokenRestClientTest.RandomPortInitializer.class,
        classes = {
                TkmRestConnectorConfig.class,
                TokenPanRestClientImpl.class,
                TokenPanRestConnector.class,
                FeignAutoConfiguration.class,
                HttpMessageConvertersAutoConfiguration.class
        }
)
public class TokenRestClientTest {

    @Autowired
    private TokenPanRestClient tokenPanRestClient;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(wireMockConfig()
            .dynamicHttpsPort()
            .dynamicPort()
            .needClientAuth(true)
            .keystorePath("src/test/resources/certs/server-keystore.jks")
            .keystorePassword("secret")
            .trustStorePath("src/test/resources/certs/server-truststore.jks")
            .trustStorePassword("secret")
            .usingFilesUnderClasspath("stubs/hpan")
    );

    @SneakyThrows
    @Test
    public void getList() {
        ((TokenPanRestClientImpl)tokenPanRestClient).setValidationDate(LocalDateTime
                .parse("Mon, 22 Jun 2020 15:58:35 GMT",
                  DateTimeFormatter.RFC_1123_DATE_TIME));
        List<File> tokenList = tokenPanRestClient.getTokenList();
        assertNotNull(tokenList);
    }

    @SneakyThrows
    @Test
    public void getList_OK_TimeEdge() {
        ((TokenPanRestClientImpl)tokenPanRestClient).setValidationDate(LocalDateTime
                .parse("Mon, 22 Jun 2020 00:00:00 GMT",
                        DateTimeFormatter.RFC_1123_DATE_TIME));
        List<File> hpanList = tokenPanRestClient.getTokenList();
        assertNotNull(hpanList);
    }


    @SneakyThrows
    @Test
    public void getList_KO_TimeExceeding() {
        ((TokenPanRestClientImpl)tokenPanRestClient).setValidationDate(LocalDateTime
                .parse("Tue, 23 Jun 2020 00:00:01 GMT",
                        DateTimeFormatter.RFC_1123_DATE_TIME));
        expectedException.expect(Exception.class);
        tokenPanRestClient.getTokenList();
    }


    @SneakyThrows
    @Test
    public void getBinList() {
        ((TokenPanRestClientImpl)tokenPanRestClient).setValidationDate(LocalDateTime
                .parse("Mon, 22 Jun 2020 15:58:35 GMT",
                        DateTimeFormatter.RFC_1123_DATE_TIME));
        List<File> binList = tokenPanRestClient.getBinList();
        assertNotNull(binList);
    }

    @SneakyThrows
    @Test
    public void getBinList_OK_TimeEdge() {
        ((TokenPanRestClientImpl)tokenPanRestClient).setValidationDate(LocalDateTime
                .parse("Mon, 22 Jun 2020 00:00:00 GMT",
                        DateTimeFormatter.RFC_1123_DATE_TIME));
        List<File> binList = tokenPanRestClient.getBinList();
        assertNotNull(binList);
    }


    @SneakyThrows
    @Test
    public void getParList_KO_TimeExceeding() {
        ((TokenPanRestClientImpl)tokenPanRestClient).setValidationDate(LocalDateTime
                .parse("Tue, 23 Jun 2020 00:00:01 GMT",
                        DateTimeFormatter.RFC_1123_DATE_TIME));
        expectedException.expect(Exception.class);
        tokenPanRestClient.getBinList();
    }



    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils
                    .addInlinedPropertiesToEnvironment(applicationContext,
                            String.format("rest-client.tkm.base-url=https://localhost:%d/hpan",
                                    wireMockRule.httpsPort())
                    );
        }
    }
}