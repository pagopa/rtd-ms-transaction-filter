package it.gov.pagopa.rtd.transaction_filter.connector.config;

import com.jcraft.jsch.ChannelSftp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.MessageHandler;

import java.io.File;

@Configuration
@IntegrationComponentScan(value = "it.gov.pagopa.rtd")
@PropertySource("classpath:config/transactionSftpChannel.properties")
public class TransactionSftpChannelConfig {

    @Value("${connectors.sftpConfigurations.connection.host}")
    private String host;
    @Value("${connectors.sftpConfigurations.connection.port}")
    private int port;
    @Value("${connectors.sftpConfigurations.connection.user}")
    private String user;
    @Value("${connectors.sftpConfigurations.connection.password}")
    private String password;
    @Value("${connectors.sftpConfigurations.connection.privateKey:#{null}}")
    private Resource privateKey;
    @Value("${connectors.sftpConfigurations.connection.passphrase}")
    private String passphrase;
    @Value("${connectors.sftpConfigurations.connection.directory}")
    private String remoteDirectory;
    @Value("${connectors.sftpConfigurations.connection.allowUnknownKeys}")
    private Boolean allowUnknownKeys;

    @Bean
    public SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(host);
        factory.setPort(port);
        factory.setUser(user);
        if (privateKey != null) {
            factory.setPrivateKey(privateKey);
            factory.setPrivateKeyPassphrase(passphrase);
        } else {
            factory.setPassword(password);
        }
        factory.setAllowUnknownKeys(allowUnknownKeys);
        return new CachingSessionFactory<>(factory);
    }

    @Bean
    @ServiceActivator(inputChannel = "sftpChannel")
    public MessageHandler handler() {
        SftpMessageHandler handler = new SftpMessageHandler(sftpSessionFactory());
        handler.setRemoteDirectoryExpression(new LiteralExpression(remoteDirectory));
        handler.setFileNameGenerator(message -> {
            if (message.getPayload() instanceof File) {
                return ((File) message.getPayload()).getName();
            } else {
                throw new IllegalArgumentException("File expected as payload.");
            }
        });
        return handler;
    }

    @MessagingGateway
    public interface TransactionSftpGateway {

        @Gateway(requestChannel = "sftpChannel")
        void sendToSftp(File file);

    }

}
