package it.gov.pagopa.rtd.transaction_filter.connector.config;

import feign.Client;
import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestConnector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;

@Configuration
@EnableFeignClients(clients = HpanRestConnector.class)
@PropertySource("classpath:config/rest-client.properties")
@Slf4j
public class HpanRestConnectorConfig {

    @Value("${rest-client.hpan.trust-store.file}")
    private String trustStoreFile;

    @Value("${rest-client.hpan.trust-store.type}")
    private String trustStoreType;

    @Value("${rest-client.hpan.trust-store.algorithm}")
    private String trustStoreAlgorithm;

    @Value("${rest-client.hpan.trust-store.password}")
    private String trustStorePassword;

    @Value("${rest-client.hpan.key-store.file}")
    private String keyStoreFile;

    @Value("${rest-client.hpan.key-store.type}")
    private String keyStoreType;

    @Value("${rest-client.hpan.key-store.algorithm}")
    private String keyStoreAlgorithm;

    @Value("${rest-client.hpan.key-store.password}")
    private String keyStorePassword;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Bean
    @ConditionalOnProperty(name = "rest-client.hpan.mtls.enabled", havingValue = "true")
    public Client getFeignClient() throws Exception {
        try {
            return new Client.Default(getSSLSocketFactory(), null);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(),e);
            }
            throw new Exception("Error in initializing feign client", e);
        }
    }

    @SneakyThrows
    private SSLSocketFactory getSSLSocketFactory() {

        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyStore trustKeyStore = KeyStore.getInstance(trustStoreType != null ?
                trustStoreType : KeyStore.getDefaultType());
        KeyManager[] keyManagers = null;

        if (trustStoreFile != null) {
            FileInputStream trustStoreFIS = new FileInputStream(resolver.getResource(trustStoreFile).getFile());
            trustKeyStore.load(trustStoreFIS, trustStorePassword.toCharArray());
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                trustStoreAlgorithm != null ? trustStoreAlgorithm : TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustKeyStore);

        if (keyStoreFile != null) {
            KeyStore keyStoreInstance = KeyStore.getInstance(keyStoreType != null ?
                    keyStoreType : KeyStore.getDefaultType());
            FileInputStream keyStoreFIS = new FileInputStream(resolver.getResource(keyStoreFile).getFile());
            keyStoreInstance.load(keyStoreFIS, keyStorePassword.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                    keyStoreAlgorithm != null ? keyStoreAlgorithm : KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStoreInstance, keyStorePassword.toCharArray());
            keyManagers = keyManagerFactory.getKeyManagers();
        }

        if (keyManagers == null) {
            keyManagers = new KeyManager[] {};
        }

        sslContext.init(keyManagers, trustManagerFactory.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }

}
