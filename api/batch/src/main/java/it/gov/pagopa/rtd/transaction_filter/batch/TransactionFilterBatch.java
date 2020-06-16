package it.gov.pagopa.rtd.transaction_filter.batch;


import it.gov.pagopa.rtd.transaction_filter.batch.step.PanReaderStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.FileManagementTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.HpanListRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.SaltRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.SftpConnectorService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowJobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Date;

/**
 * Configuration of a scheduled batch job to read and decrypt .pgp files with csv content,
 * to be processed in instances of Transaction class, to be sent in an outbound Kafka channel
 */

@Configuration
@PropertySource("classpath:config/transactionFilterBatch.properties")
@Import({TransactionFilterStep.class,PanReaderStep.class})
@EnableBatchProcessing
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class TransactionFilterBatch {

    private final TransactionFilterStep transactionFilterStep;
    private final PanReaderStep panReaderStep;
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final BeanFactory beanFactory;
    private final HpanConnectorService hpanConnectorService;
    private final SftpConnectorService sftpConnectorService;

    @Value("${batchConfiguration.TransactionFilterBatch.successArchivePath}")
    private String successArchivePath;
    @Value("${batchConfiguration.TransactionFilterBatch.errorArchivePath}")
    private String errorArchivePath;
    @Value("${batchConfiguration.TransactionFilterBatch.tablePrefix}")
    private String tablePrefix;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.directoryPath}")
    private String hpanListDirectory;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.filename}")
    private String hpanListFilename;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.attemptExtract}")
    private Boolean attemptExtract;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.checksumFilePattern}")
    private String checksumFilePattern;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.listFilePattern}")
    private String hpanListPattern;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.deleteLocalFiles}")
    private Boolean deleteLocalFiles;
    @Value("${batchConfiguration.TransactionFilterBatch.saltRecovery.enabled}")
    private Boolean saltRecoveryEnabled;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled}")
    private Boolean hpanListRecoveryEnabled;

    private DataSource dataSource;
    private HpanStoreService hpanStoreService;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /**
     * Scheduled method used to launch the configured batch job for processing transaction from a defined directory.
     * The scheduler is based on a cron execution, based on the provided configuration
     * @throws Exception
     */
    @Scheduled(cron = "${batchConfiguration.TransactionFilterBatch.cron}")
    public void launchJob() throws Exception {

        Date startDate = new Date();
        if (log.isInfoEnabled()) {
            log.info("CsvTransactionReader scheduled job started at " + startDate);
        }

        Resource[] transactionResources = resolver.getResources(transactionFilterStep.getTransactionDirectoryPath());

        if (transactionResources.length > 0) {
            hpanStoreService = batchHpanStoreService();
            jobLauncher().run(
                    job(),
                    new JobParametersBuilder()
                            .addDate("startDateTime", startDate)
                            .toJobParameters());
            hpanStoreService.clearAll();
        }

        Date endDate = new Date();
        if (log.isInfoEnabled()) {
            log.info("CsvTransactionReader scheduled job ended at " + endDate);
            log.info("Completed in: " + (endDate.getTime() - startDate.getTime()) + " (ms)");
        }

    }

    /**
     *
     * @return configured instance of TransactionManager
     */
    @Bean
    public PlatformTransactionManager getTransactionManager() {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);
        return dataSourceTransactionManager;
    }

    /**
     *
     * @return configured instance of JobRepository
     * @throws Exception
     */
    @Bean
    public JobRepository getJobRepository() throws Exception {
            JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
            jobRepositoryFactoryBean.setTransactionManager(getTransactionManager());
            jobRepositoryFactoryBean.setTablePrefix(tablePrefix);
            jobRepositoryFactoryBean.setDataSource(dataSource);
            jobRepositoryFactoryBean.afterPropertiesSet();
            return jobRepositoryFactoryBean.getObject();
    }

    /**
     *
     * @return configured instance of JobLauncher
     * @throws Exception
     */
    @Bean
    public JobLauncher jobLauncher() throws Exception {
        SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
        simpleJobLauncher.setJobRepository(getJobRepository());
        return simpleJobLauncher;
    }

    /**
     *
     * @return instance of a job for transaction processing
     */
    @SneakyThrows
    @Bean
    public Job job() {
        return transactionJobBuilder().build();
    }

    /**
     *
     * @return bean for a ThreadPoolTaskScheduler
     */
    @Bean
    public TaskScheduler poolScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     *
     * @return instance of the job to process and archive .pgp files containing Transaction data in csv format
     */
    @SneakyThrows
    public FlowJobBuilder transactionJobBuilder() {
        return jobBuilderFactory.get("transaction-filter-job")
                .repository(getJobRepository())
                .start(hpanListRecoveryTask())
                .on("FAILED").end()
                .from(hpanListRecoveryTask()).on("*").to(saltRecoveryTask())
                .on("FAILED").end()
                .from(saltRecoveryTask()).on("*")
                .to(panReaderStep.hpanRecoveryMasterStep(this.hpanStoreService))
                .on("FAILED").to(fileManagementTask())
                .from(panReaderStep.hpanRecoveryMasterStep(this.hpanStoreService))
                .on("*").to(transactionFilterStep.transactionFilterMasterStep(this.hpanStoreService))
                .from(transactionFilterStep.transactionFilterMasterStep(this.hpanStoreService))
                .on("FAILED").to(fileManagementTask())
                .from(transactionFilterStep.transactionFilterMasterStep(this.hpanStoreService))
                .on("*").to(transactionFilterStep.transactionSenderMasterStep(
                        this.sftpConnectorService))
                .on("*").to(fileManagementTask())
                .build();
    }

    public Step hpanListRecoveryTask() {
        HpanListRecoveryTasklet hpanListRecoveryTasklet = new HpanListRecoveryTasklet();
        hpanListRecoveryTasklet.setHpanListDirectory(hpanListDirectory);
        hpanListRecoveryTasklet.setHpanConnectorService(hpanConnectorService);
        hpanListRecoveryTasklet.setFileName(hpanListFilename);
        hpanListRecoveryTasklet.setChecksumFilePattern(checksumFilePattern);
        hpanListRecoveryTasklet.setExtractFile(attemptExtract);
        hpanListRecoveryTasklet.setListFilePattern(hpanListPattern);
        hpanListRecoveryTasklet.setTaskletEnabled(hpanListRecoveryEnabled);
        return stepBuilderFactory
                .get("transaction-filter-salt-hpan-list-recovery-step")
                .tasklet(hpanListRecoveryTasklet).build();
    }

    public Step saltRecoveryTask() {
        SaltRecoveryTasklet saltRecoveryTasklet = new SaltRecoveryTasklet();
        saltRecoveryTasklet.setHpanConnectorService(hpanConnectorService);
        saltRecoveryTasklet.setTaskletEnabled(saltRecoveryEnabled);
        return stepBuilderFactory.get("transaction-filter-salt-recovery-step")
                .tasklet(saltRecoveryTasklet).listener(promotionListener()).build();
    }

    /**
     *
     * @return step instance based on the tasklet to be used for file archival at the end of the reading process
     */
    @SneakyThrows
    @Bean
    public Step fileManagementTask() {
        FileManagementTasklet fileManagementTasklet = new FileManagementTasklet();
        fileManagementTasklet.setSuccessPath(successArchivePath);
        fileManagementTasklet.setErrorPath(errorArchivePath);
        fileManagementTasklet.setHpanDirectory(panReaderStep.getHpanDirectoryPath());
        fileManagementTasklet.setOutputDirectory(transactionFilterStep.getOutputDirectoryPath());
        fileManagementTasklet.setDeleteLocalFiles(deleteLocalFiles);
        return stepBuilderFactory.get("transaction-filter-file-management-step")
                .tasklet(fileManagementTasklet).build();
    }

    @Bean
    public ExecutionContextPromotionListener promotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[] {"salt"});
        return listener;
    }

    public HpanStoreService batchHpanStoreService() {
        return beanFactory.getBean(HpanStoreService.class);
    }

}
