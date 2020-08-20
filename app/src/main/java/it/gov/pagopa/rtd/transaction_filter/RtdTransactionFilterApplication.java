package it.gov.pagopa.rtd.transaction_filter;

import it.gov.pagopa.rtd.transaction_filter.batch.TransactionFilterBatch;
import it.gov.pagopa.rtd.transaction_filter.batch.step.PanReaderStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.Date;

/**
 * Main class for microservice startup
 */
@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class, SessionAutoConfiguration.class})
@ComponentScan(basePackages = {"it.gov.pagopa.rtd"})
@Slf4j
@RequiredArgsConstructor
public class RtdTransactionFilterApplication implements CommandLineRunner {

	@Value("${spring.batch.job.scheduled}")
	private String scheduledEnabled;

	private final TransactionFilterStep transactionFilterStep;
	private final PanReaderStep panReaderStep;
	private final TransactionFilterBatch transactionFilterBatch;
	PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	public static void main(String[] args) {
		SpringApplication.run(RtdTransactionFilterApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

    // FIXME: this check should be case insensitive, the README states that the
    //        allowed values are "TRUE" and "FALSE"
		if (scheduledEnabled.equals("false")) {

			Date startDate = new Date();
			if (log.isInfoEnabled()) {
				log.info("CsvTransactionReader single-time job started at " + startDate);
			}

			transactionFilterBatch.executeBatchJob(startDate);

			Date endDate = new Date();
			if (log.isInfoEnabled()) {
				log.info("CsvTransactionReader single-time job ended at " + endDate);
				log.info("Completed in: " + (endDate.getTime() - startDate.getTime()) + " (ms)");
			}

			System.exit(0);

		}

	}
}
