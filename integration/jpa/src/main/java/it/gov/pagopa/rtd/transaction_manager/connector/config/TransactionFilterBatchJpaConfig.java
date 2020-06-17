package it.gov.pagopa.rtd.transaction_manager.connector.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * Configuration class for the MEDA JpaConnector
 */
@Configuration
@PropertySource("classpath:config/jpaConnectionConfig.properties")
@EntityScan(basePackages = "it.gov.pagopa")
@EnableJpaRepositories(
        repositoryBaseClass = SimpleJpaRepository.class,
        basePackages = {"it.gov.pagopa"}
)
public class TransactionFilterBatchJpaConfig { }
