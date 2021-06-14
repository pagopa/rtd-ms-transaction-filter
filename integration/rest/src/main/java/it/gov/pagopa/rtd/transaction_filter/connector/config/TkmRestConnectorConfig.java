package it.gov.pagopa.rtd.transaction_filter.connector.config;

import feign.Client;
import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestConnector;
import it.gov.pagopa.rtd.transaction_filter.connector.TokenPanRestConnector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.security.KeyStore;

@Configuration
@EnableFeignClients(clients = TokenPanRestConnector.class)
@PropertySource("classpath:config/rest-tkm-client.properties")
@Slf4j
public class TkmRestConnectorConfig {

    @Value("${rest-client.tkm.mtls.enabled}")
    private Boolean mtlsEnabled;

    @Value("${rest-client.tkm.proxy.enabled}")
    private Boolean proxyEnabled;

    @Value("${rest-client.tkm.proxy.host}")
    private String proxyHost;

    @Value("${rest-client.tkm.proxy.port}")
    private Integer proxyPort;

    @Value("${rest-client.tkm.proxy.username}")
    private String proxyUsername;

    @Value("${rest-client.tkm.proxy.password}")
    private String proxyPassword;

    @Value("${rest-client.tkm.trust-store.file}")
    private String trustStoreFile;

    @Value("${rest-client.tkm.trust-store.type}")
    private String trustStoreType;

    @Value("${rest-client.tkm.trust-store.algorithm}")
    private String trustStoreAlgorithm;

    @Value("${rest-client.tkm.trust-store.password}")
    private String trustStorePassword;

    @Value("${rest-client.tkm.key-store.file}")
    private String keyStoreFile;

    @Value("${rest-client.tkm.key-store.type}")
    private String keyStoreType;

    @Value("${rest-client.tkm.key-store.algorithm}")
    private String keyStoreAlgorithm;

    @Value("${rest-client.tkm.key-store.password}")
    private String keyStorePassword;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Bean
    public Client getFeignClient() throws Exception {
        try {
            SSLSocketFactory sslSocketFactory = null;

            if (mtlsEnabled) {
                sslSocketFactory = getSSLSocketFactory();
                log.debug("enabled socket factory: {}", sslSocketFactory);
            }

            if (proxyEnabled) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                if (proxyUsername != null && !proxyUsername.equals("") &&
                        proxyPassword != null && !proxyPassword.equals("")) {
                    Client client = new Client.Proxied(sslSocketFactory,null, proxy,
                            proxyUsername, proxyPassword);
                    System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
                    Authenticator.setDefault(new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(proxyUsername, proxyPassword.toCharArray());
                        }
                    });
                    return client;
                } else {
                    return new Client.Proxied(sslSocketFactory,null, proxy);
                }

            } else {
                return new Client.Default(sslSocketFactory, null);
            }
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            throw new Exception("Error occured while initializing feign client", e);
        }
    }

    @SneakyThrows
    private SSLSocketFactory getSSLSocketFactory() {

        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyStore trustKeyStore = KeyStore.getInstance(trustStoreType != null ?
                trustStoreType : KeyStore.getDefaultType());
        KeyManager[] keyManagers = null;

        if (trustStoreFile != null && !trustStoreFile.equals("file:/")) {
            FileInputStream trustStoreFIS = new FileInputStream(resolver.getResource(trustStoreFile).getFile());
            trustKeyStore.load(trustStoreFIS, trustStorePassword.toCharArray());
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                trustStoreAlgorithm != null ? trustStoreAlgorithm : TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustKeyStore);

        if (keyStoreFile != null && !keyStoreFile.equals("file:/")) {
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
