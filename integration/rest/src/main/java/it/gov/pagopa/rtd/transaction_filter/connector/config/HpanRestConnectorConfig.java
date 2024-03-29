package it.gov.pagopa.rtd.transaction_filter.connector.config;

import feign.Client;
import feign.RequestInterceptor;
import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestConnector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
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
@EnableFeignClients(clients = HpanRestConnector.class)
@PropertySource("classpath:config/rest-client.properties")
@Slf4j
public class HpanRestConnectorConfig {

    @Value("${rest-client.hpan.mtls.enabled}")
    private Boolean mtlsEnabled;

    @Value("${rest-client.hpan.proxy.enabled}")
    private Boolean proxyEnabled;

    @Value("${rest-client.hpan.proxy.host}")
    private String proxyHost;

    @Value("${rest-client.hpan.proxy.port}")
    private Integer proxyPort;

    @Value("${rest-client.hpan.proxy.username}")
    private String proxyUsername;

    @Value("${rest-client.hpan.proxy.password}")
    private String proxyPassword;

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

    @Value("${rest-client.user-agent.prefix}")
    private String userAgentHeaderPrefix;

    @Value("${rest-client.user-agent.version}")
    private String userAgentVersion;

    @Value("${apache.httpClient.config.socketTimeoutInSeconds:180}")
    private Integer socketTimeoutInSeconds;
    @Value("${apache.httpClient.config.connectTimeoutInSeconds:180}")
    private Integer connectTimeoutInSeconds;
    @Value("${apache.httpClient.config.timeToLiveInSeconds:600}")
    private Integer timeToLiveInSeconds;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public String getUserAgent() {
        return userAgentHeaderPrefix + "/" + userAgentVersion;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        // This interceptor injects the User-Agent header in each request made with the client.
        return requestTemplate -> requestTemplate.header("User-Agent", getUserAgent());

    }

    @Bean
    public Client getFeignClient() {
        SSLSocketFactory sslSocketFactory = null;

        if (Boolean.TRUE.equals(mtlsEnabled)) {
            sslSocketFactory = getSSLContext().getSocketFactory();
            log.debug("enabled socket factory: {}", sslSocketFactory);
        }

        if (Boolean.TRUE.equals(proxyEnabled)) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            if (proxyUsername != null && !proxyUsername.equals("") &&
                    proxyPassword != null && !proxyPassword.equals("")) {
                Client client = new Client.Proxied(sslSocketFactory,null, proxy,
                        proxyUsername, proxyPassword);
                System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
                Authenticator.setDefault(new Authenticator() {
                    @Override
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
    }

    @Bean
    public PoolingHttpClientConnectionManager getConnectionManager() {
        return PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(getSSLContext())
                .setTlsVersions(TLS.V_1_2)
                .build())
            .setDefaultSocketConfig(SocketConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(socketTimeoutInSeconds))
                .build())
            .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
            .setConnPoolPolicy(PoolReusePolicy.LIFO)
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                .setSocketTimeout(Timeout.ofSeconds(socketTimeoutInSeconds))
                .setConnectTimeout(Timeout.ofSeconds(connectTimeoutInSeconds))
                .setTimeToLive(TimeValue.ofSeconds(timeToLiveInSeconds))
                .build())
            .build();
    }

    @SneakyThrows
    public SSLContext getSSLContext() {

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
            keyManagers = new KeyManager[]{};
        }

        sslContext.init(keyManagers, trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }

}
