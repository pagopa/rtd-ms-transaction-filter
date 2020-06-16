package it.gov.pagopa.rtd.transaction_manager.connector.config;

import it.gov.pagopa.bpd.common.connector.jpa.config.BaseJpaConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuration class for the MEDA JpaConnector
 */
@Configuration
@PropertySource("classpath:config/jpaConnectionConfig.properties")
public class TransactionFilterBatchJpaConfig extends BaseJpaConfig { }
