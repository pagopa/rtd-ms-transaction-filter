package it.gov.pagopa.rtd.transaction_filter;

import it.gov.pagopa.rtd.transaction_filter.batch.BatchExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.support.SimpleJvmExitCodeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

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

	private final BatchExecutor batchExecutor;

	public static void main(String[] args) {
		SpringApplication.run(RtdTransactionFilterApplication.class, args);
	}

	@Override
	public void run(String... args) {

		if (scheduledEnabled.equalsIgnoreCase("false")) {

			Date startDate = new Date();
			if (log.isInfoEnabled()) {
				log.info("CsvTransactionReader single-time job started at " + startDate);
			}
			JobExecution jobExecution = batchExecutor.execute(startDate);

			Date endDate = new Date();
			if (log.isInfoEnabled()) {
				log.info("CsvTransactionReader single-time job ended at " + endDate);
				log.info("Completed in: " + (endDate.getTime() - startDate.getTime()) + " (ms)");
			}

			System.exit(jobExecution != null ?
					new SimpleJvmExitCodeMapper().intValue(jobExecution.getExitStatus().getExitCode()) : 0);

		}

	}
}
