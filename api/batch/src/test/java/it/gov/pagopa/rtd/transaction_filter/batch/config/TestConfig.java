package it.gov.pagopa.rtd.transaction_filter.batch.config;

import it.gov.pagopa.rtd.transaction_manager.connector.config.TransactionFilterBatchJpaConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Test configuration class for api/event
 */

@ComponentScan(basePackages = {"it.gov.pagopa"}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = TransactionFilterBatchJpaConfig.class)
})
public class TestConfig {
}
