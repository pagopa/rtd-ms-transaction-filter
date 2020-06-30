package it.gov.pagopa.rtd.transaction_filter.connector;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import it.gov.pagopa.rtd.transaction_filter.connector.config.HpanRestConnectorConfig;
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

import java.io.File;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
        locations = "classpath:config/rest-client.properties",
        properties = {
                "rest-client.hpan.list.url=/list",
                "rest-client.hpan.salt.url=/salt",
                "rest-client.hpan.mtls.enabled=true",
                "rest-client.hpan.list.checksumHeaderName=checksum",
                "rest-client.hpan.key-store.file=classpath:certs/client-keystore.jks",
                "rest-client.hpan.key-store.password=secret",
                "rest-client.hpan.trust-store.file=classpath:certs/client-truststore.jks",
                "rest-client.hpan.trust-store.password=secret",
                "spring.application.name=rtd-ms-transaction-filter-integration-rest"
        }
)
@ContextConfiguration(initializers = HpanRestClientTest.RandomPortInitializer.class,
        classes = {
            HpanRestConnectorConfig.class,
            HpanRestClientImpl.class,
            HpanRestConnector.class,
            FeignAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class
        }
)
public class HpanRestClientTest {

    @Autowired
    private HpanRestClient hpanRestClient;

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(wireMockConfig()
            .dynamicHttpsPort()
            .needClientAuth(true)
            .keystorePath("src/test/resources/certs/server-keystore.jks")
            .keystorePassword("secret")
            .trustStorePath("src/test/resources/certs/server-truststore.jks")
            .trustStorePassword("secret")
            .usingFilesUnderClasspath("stubs/hpan")
    );

    @Test
    public void getSalt() {
        String salt = hpanRestClient.getSalt();
        assertNotNull(salt);
    }

    @Test
    public void getList() {
        File hpanList = hpanRestClient.getList();
        assertNotNull(hpanList);
    }


    public static class RandomPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils
                    .addInlinedPropertiesToEnvironment(applicationContext,
                            String.format("rest-client.hpan.base-url=https://localhost:%d/hpan",
                                    wireMockRule.httpsPort())
                    );
        }
    }
}