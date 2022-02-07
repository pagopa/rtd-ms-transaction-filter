package it.gov.pagopa.rtd.transaction_filter.connector;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import it.gov.pagopa.rtd.transaction_filter.connector.config.HpanRestConnectorConfig;
import lombok.SneakyThrows;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
        locations = "classpath:config/rest-client.properties",
        properties = {
                "rest-client.hpan.list.url=/rtd/payment-instrument-manager/hashed-pans",
                "rest-client.hpan.salt.url=/rtd/payment-instrument-manager/salt",
                "rest-client.hpan.adesas.url=/rtd/csv-transaction/ade/sas",
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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(Objects.requireNonNull(getClass().getResource("/")).getFile()));

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
    public void getSalt() {
        String salt = hpanRestClient.getSalt();
        assertNotNull(salt);
    }

    @SneakyThrows
    @Test
    public void getList() {
        ((HpanRestClientImpl) hpanRestClient).setValidationDate(LocalDateTime
                .parse("Mon, 22 Jun 2020 15:58:35 GMT",
                        DateTimeFormatter.RFC_1123_DATE_TIME));
        File hpanList = hpanRestClient.getList();
        assertNotNull(hpanList);
    }

    @SneakyThrows
    @Test
    public void getListOnTimeEdge() {
        ((HpanRestClientImpl) hpanRestClient).setValidationDate(LocalDateTime
                .parse("Mon, 22 Jun 2020 00:00:00 GMT",
                        DateTimeFormatter.RFC_1123_DATE_TIME));
        File hpanList = hpanRestClient.getList();
        assertNotNull(hpanList);
    }

    @SneakyThrows
    @Test
    public void getListRaisesExceptionWhenTimeExceeding() {
        ((HpanRestClientImpl) hpanRestClient).setValidationDate(LocalDateTime
                .parse("Tue, 23 Jun 2020 00:00:01 GMT",
                        DateTimeFormatter.RFC_1123_DATE_TIME));
        expectedException.expect(Exception.class);
        hpanRestClient.getList();
    }

    @Test
    public void getSasTokenForAdeScope() {
        SasResponse sas = hpanRestClient.getSasToken(HpanRestClient.SasScope.ADE);
        SasResponse expectedSas = new SasResponse();
        expectedSas.setSas("sig=1FKx%2F7lrOhV4YidvHmuW8rMP4lCG%2BqX1pri%2FApjXJok%3D&st=2022-01-25T07:17Z&se=2022-01-25T08:17Z&spr=https&sp=rcw&sr=c&sv=2020-12-06");
        expectedSas.setAuthorizedContainer("ade-transactions-116fecdd119fa27327d00bfbb975ece53e9c1d007a7e");
        assertEquals(expectedSas, sas);
    }

    @Test
    public void getSasTokenForCstarScope() {
        SasResponse sas = hpanRestClient.getSasToken(HpanRestClient.SasScope.CSTAR);
        SasResponse expectedSas = new SasResponse();
        expectedSas.setSas("sig=2GKx%2F7lrOhV4YidvHmuW8rMP4lCG%2BqX1pri%2FApjXJok%3D&st=2022-01-25T07:17Z&se=2022-01-25T08:17Z&spr=https&sp=rcw&sr=c&sv=2020-12-06");
        expectedSas.setAuthorizedContainer("cstar-transactions-216fecdd119fa27327d00bfbb975ece53e9c1d007a7e");
        assertEquals(expectedSas, sas);
    }

    @Test
    public void uploadFile() throws IOException {
        File fileToUpload = tempFolder.newFile("testFile");
        assertNull(hpanRestClient.uploadFile(fileToUpload, "sas-token", "authorized-container"));
    }

    @Test
    public void getPublicKey() {
        String publicKey = hpanRestClient.getPublicKey();
        assertEquals("keycontent", publicKey);
    }

    @Test
    public void uploadFileRaisesExceptionWhenSignatureDoesntMatch() throws IOException {
        File fileToUpload = tempFolder.newFile("testFile");
        expectedException.expect(IOException.class);
        hpanRestClient.uploadFile(fileToUpload, "sas-token", "not-authorized-container");
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