package it.gov.pagopa.rtd.transaction_filter;

import it.gov.pagopa.rtd.transaction_filter.batch.TokenPanFilterBatch;
import it.gov.pagopa.rtd.transaction_filter.batch.TransactionFilterBatch;
import it.gov.pagopa.rtd.transaction_filter.batch.step.*;
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
	private final ParReaderStep parReaderStep;
	private final TokenPanFilterStep tokenPanFilterStep;
	private final BinReaderStep binReaderStep;
	private final TokenPanReaderStep tokenPanReaderStep;
	private final TransactionFilterBatch transactionFilterBatch;
	private final TokenPanFilterBatch tokenPanFilterBatch;
	PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	public static void main(String[] args) {
		SpringApplication.run(RtdTransactionFilterApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		if (scheduledEnabled.toLowerCase().equals("false")) {

			Date startDate = new Date();
			if (log.isInfoEnabled()) {
				log.info("CsvTransactionReader single-time job started at " + startDate);
			}

			JobExecution jobExecution1 = transactionFilterBatch.executeBatchJob(startDate);

			int jobExecCode = jobExecution1 != null ?
					new SimpleJvmExitCodeMapper().intValue(
							jobExecution1.getExitStatus().getExitCode()) : 0;


			if (jobExecCode != 0 && (tokenPanFilterBatch.getTokenPanListRecoveryEnabled() ||
					tokenPanFilterBatch.getBinListDailyRemovalEnabled())) {
				JobExecution jobExecution2 = tokenPanFilterBatch.executeBatchJob(startDate);
				jobExecCode = jobExecution2 != null ?
						new SimpleJvmExitCodeMapper().intValue(
								jobExecution2.getExitStatus().getExitCode()) : 0;

			}

			Date endDate = new Date();
			if (log.isInfoEnabled()) {
				log.info("CsvTransactionReader single-time job ended at " + endDate);
				log.info("Completed in: " + (endDate.getTime() - startDate.getTime()) + " (ms)");
			}

			System.exit(jobExecCode);

		}

	}
}
