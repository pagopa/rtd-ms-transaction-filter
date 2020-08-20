package it.gov.pagopa.rtd.transaction_filter.batch;


import it.gov.pagopa.rtd.transaction_filter.batch.step.PanReaderStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.FileManagementTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.HpanListRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.SaltRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.SftpConnectorService;
import lombok.Data;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Date;

/**
 * Configuration of a scheduled batch job to read and decrypt .pgp files with csv content,
 * to be processed in instances of Transaction class, to be sent in an outbound Kafka channel
 */

@Configuration
@Data
@PropertySource("classpath:config/transactionFilterBatch.properties")
@Import({TransactionFilterStep.class,PanReaderStep.class})
@EnableBatchProcessing
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

    @Value("${batchConfiguration.TransactionFilterBatch.isolationForCreate}")
    private String isolationForCreate;
    @Value("${batchConfiguration.TransactionFilterBatch.successArchivePath}")
    private String successArchivePath;
    @Value("${batchConfiguration.TransactionFilterBatch.errorArchivePath}")
    private String errorArchivePath;
    @Value("${batchConfiguration.TransactionFilterBatch.tablePrefix}")
    private String tablePrefix;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.directoryPath}")
    private String hpanListDirectory;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.filePattern}")
    private String hpanListRecoveryFilePattern;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.filename}")
    private String hpanListFilename;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.deleteProcessedFiles}")
    private Boolean deleteProcessedFiles;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.deleteOutputFiles}")
    private String deleteOutputFiles;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.manageHpanOnSuccess}")
    private String manageHpanOnSuccess;
    @Value("${batchConfiguration.TransactionFilterBatch.saltRecovery.enabled}")
    private Boolean saltRecoveryEnabled;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled}")
    private Boolean hpanListRecoveryEnabled;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanListRecovery.dailyRemoval.enabled}")
    private Boolean hpanListDailyRemovalEnabled;


    private DataSource dataSource;
    private HpanStoreService hpanStoreService;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public void createHpanStoreService() {
        this.hpanStoreService = batchHpanStoreService();
    }

    public void clearHpanStoreService() {
        hpanStoreService.clearAll();
    }

    // FIXME: please comment why we need to throw checked exceptions
    @SneakyThrows
    public void executeBatchJob(Date startDate) {
        String transactionsPath = transactionFilterStep.getTransactionDirectoryPath();
        Resource[] transactionResources = resolver.getResources(transactionsPath);

        String hpanPath = panReaderStep.getHpanDirectoryPath();
        Resource[] hpanResources = resolver.getResources(hpanPath);

        // FIXME: please add a short explanation of the following logic since
        //        it's not easy to understand
        if (transactionResources.length > 0 &&
                (getHpanListRecoveryEnabled() || hpanResources.length>0)) {

            if (log.isInfoEnabled()) {
                log.info("Found " + transactionResources.length +
                        (transactionResources.length > 1 ? "resources" : "resource") +
                        ". Starting filtering process"
                );
            }

            createHpanStoreService();
            jobLauncher().run(job(),
                    new JobParametersBuilder()
                            .addDate("startDateTime", startDate)
                            .toJobParameters());
            clearHpanStoreService();

        } else {
            if (log.isInfoEnabled()) {
                log.info("No transaction file has been found on configured path: " + transactionsPath);
                if (getHpanListRecoveryEnabled() && hpanResources.length==0) {
                    log.info("No hpan file has been found on configured path: " + hpanPath);
                }
            }
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
            if (isolationForCreate != null) {
                jobRepositoryFactoryBean.setIsolationLevelForCreate(isolationForCreate);
            }
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
    // FIXME: please comment why we need SneakyThrows here
    @SneakyThrows
    public FlowJobBuilder transactionJobBuilder() {
        // FIXME: please document the following flow, it's hard to understand
        //        what's going on
        return jobBuilderFactory.get("transaction-filter-job")
                .repository(getJobRepository())
                .start(hpanListRecoveryTask())
                .on("FAILED").end()
                .from(hpanListRecoveryTask()).on("*").to(saltRecoveryTask(this.hpanStoreService))
                .on("FAILED").end()
                .from(saltRecoveryTask(this.hpanStoreService)).on("*")
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

    @Bean
    public Step hpanListRecoveryTask() {
        HpanListRecoveryTasklet hpanListRecoveryTasklet = new HpanListRecoveryTasklet();
        hpanListRecoveryTasklet.setHpanListDirectory(hpanListDirectory);
        hpanListRecoveryTasklet.setHpanConnectorService(hpanConnectorService);
        hpanListRecoveryTasklet.setFileName(hpanListFilename);
        hpanListRecoveryTasklet.setHpanFilePattern(hpanListRecoveryFilePattern);
        hpanListRecoveryTasklet.setDailyRemovalTaskletEnabled(hpanListDailyRemovalEnabled);
        hpanListRecoveryTasklet.setRecoveryTaskletEnabled(hpanListRecoveryEnabled);
        return stepBuilderFactory
                .get("transaction-filter-salt-hpan-list-recovery-step")
                .tasklet(hpanListRecoveryTasklet).build();
    }

    @Bean
    public Step saltRecoveryTask(HpanStoreService hpanStoreService) {
        SaltRecoveryTasklet saltRecoveryTasklet = new SaltRecoveryTasklet();
        saltRecoveryTasklet.setHpanConnectorService(hpanConnectorService);
        saltRecoveryTasklet.setHpanStoreService(hpanStoreService);
        saltRecoveryTasklet.setTaskletEnabled(saltRecoveryEnabled);
        return stepBuilderFactory.get("transaction-filter-salt-recovery-step")
                .tasklet(saltRecoveryTasklet).build();
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
        fileManagementTasklet.setDeleteProcessedFiles(deleteProcessedFiles);
        fileManagementTasklet.setDeleteOutputFiles(deleteOutputFiles);
        fileManagementTasklet.setManageHpanOnSuccess(manageHpanOnSuccess);
        return stepBuilderFactory.get("transaction-filter-file-management-step")
                .tasklet(fileManagementTasklet).build();
    }

    public HpanStoreService batchHpanStoreService() {
        return beanFactory.getBean(HpanStoreService.class);
    }

}
