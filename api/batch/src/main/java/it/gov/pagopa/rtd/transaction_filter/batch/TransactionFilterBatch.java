package it.gov.pagopa.rtd.transaction_filter.batch;


import it.gov.pagopa.rtd.transaction_filter.batch.step.PanReaderStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.JobListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.FileManagementTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.HpanListRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.PagopaPublicKeyRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.SaltRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.SftpConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterServiceImpl;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowJobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
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
 * <p>
 * Batch responsible for the filtering and secure transmission of transaction files provided by the acquirers.
 * @see TransactionFilterBatch#transactionJobBuilder() for the actual flow definition.
 * </p>
 *
 * <img alt="TransactionFilterBatch" src="uml/transactionFilterBatch.svg">
 * 
 * @plantUml uml/transactionFilterBatch.svg
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

    private final static String FAILED = "FAILED";

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
    @Value("${batchConfiguration.TransactionFilterBatch.pagopaPublicKeyRecovery.enabled}")
    private Boolean pagopaPublicKeyRecoveryEnabled;

    private DataSource dataSource;
    private StoreService storeService;
    private TransactionWriterServiceImpl transactionWriterService;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public void transactionWriterService() {
        transactionWriterService = beanFactory.getBean(TransactionWriterServiceImpl.class);
    }

    public void closeChannels() {
        transactionWriterService.closeAll();
    }

    public void createStoreService() {
        this.storeService = batchStoreService();
    }

    public void clearStoreService() {
        storeService.clearAll();
    }

    /**
     *
     * @return Method to start the execution of the transaction filter job
     * @param startDate starting date for the batch job execution
     */
    @SneakyThrows
    public JobExecution executeBatchJob(Date startDate) {
        Resource[] transactionResources = resolver.getResources(transactionFilterStep.getTransactionDirectoryPath() + "/*.csv");
        transactionResources = TransactionFilterStep.filterValidFilenames(transactionResources);

        String hpanPath = panReaderStep.getHpanDirectoryPath();
        Resource[] hpanResources = resolver.getResources(hpanPath);

        JobExecution execution = null;

        /*
          The jobLauncher run method is called only if, based on the configured properties, a matching transaction
          resource is found, and either the remote pan list recovery is enabled, or a pan list file is available locally
          on the configured path
         */
        if (transactionResources.length > 0 &&
                (getHpanListRecoveryEnabled() || hpanResources.length>0)) {

            log.info("Found {} {}. Starting filtering process",
                    transactionResources.length, (transactionResources.length > 1 ? "resources" : "resource")
            );

            if (transactionWriterService == null) {
                transactionWriterService();
            }
            createStoreService();
            execution = jobLauncher().run(job(),
                    new JobParametersBuilder()
                            .addDate("startDateTime", startDate)
                            .toJobParameters());
            closeChannels();
            clearStoreService();

        } else {
            if (transactionResources.length == 0) {
                log.info("No transaction file has been found on configured path: {}", transactionFilterStep.getTransactionDirectoryPath());
            }
            if (!getHpanListRecoveryEnabled() && hpanResources.length==0) {
                log.info("No hpan file has been found on configured path: {}", hpanPath);
            }
        }

        return execution;

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
     *  exception description
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
     *  exception description
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
     * This method builds a flow which can be decomposed in the following  
     * steps:
     * <ol>
     * <li>Input transactions are filtered from unneeded fields (e.g. hashpan)
     * and an output file for AdE is produced.</li>
     * <li>The output file for AdE is sent remotely via REST, if enabled.</li>
     * <li>Attempts panlist recovery, if enabled. In case of a failure in the 
     * execution, the process is stopped.</li>
     * <li>Attempts salt recovery, if enabled. In case of a failure in the 
     * execution, the process is stopped. Otherwise, the panList step is 
     * executed</li>
     * <li>The panList step is executed, to store the .csv files including the 
     * list of active pans. If the step fails, the file archival tasklet is 
     * called, otherwise the transaction filter step is called.</li>
     * <li>The transaction filter step checks the records with the stored pans, 
     * writing the matching records in the output file. If the process fails, 
     * the file management tasklet is called, otherwise the transaction sender 
     * step si called.</li>
     * <li>Attempts sending the output files through an REST channel, if
     * enabled. The file management tasklet is always called, after the step
     * </li>
     * <li>After all the possible file management executions, the process is stopped</li>
     * </ol>
     * @return Instance of {@link FlowJobBuilder}, with the configured steps executed
     * for the pan/transaction processing
     */
    @SneakyThrows
    public FlowJobBuilder transactionJobBuilder() {

        return jobBuilderFactory.get("transaction-filter-job")
                .repository(getJobRepository())
                .listener(jobListener())
                .start(pagopaPublicKeyRecoveryTask(this.storeService))
                .on(FAILED).end()
                .on("*").to(transactionFilterStep.transactionFilterAdeMasterStep(this.storeService, this.transactionWriterService))
                .on(FAILED).to(fileManagementTask())
                .from(transactionFilterStep.transactionFilterAdeMasterStep(this.storeService, this.transactionWriterService))
                .on("*").to(transactionFilterStep.transactionSenderAdeMasterStep(this.hpanConnectorService))
                .on(FAILED).to(fileManagementTask())
                .from(transactionFilterStep.transactionSenderAdeMasterStep(this.hpanConnectorService))
                .on("*").to(hpanListRecoveryTask())
                .on(FAILED).to(fileManagementTask())
                .from(hpanListRecoveryTask()).on("*").to(saltRecoveryTask(this.storeService))
                .on(FAILED).to(fileManagementTask())
                .from(saltRecoveryTask(this.storeService)).on("*")
                .to(panReaderStep.hpanRecoveryMasterStep(this.storeService))
                .on(FAILED).to(fileManagementTask())
                .from(panReaderStep.hpanRecoveryMasterStep(this.storeService))
                .on("*").to(transactionFilterStep.transactionFilterMasterStep(this.storeService,this.transactionWriterService))
                .on(FAILED).to(fileManagementTask())
                .from(transactionFilterStep.transactionFilterMasterStep(this.storeService,this.transactionWriterService))
                .on("*").to(transactionFilterStep.transactionSenderCstarMasterStep(this.hpanConnectorService))
                .on(FAILED).to(fileManagementTask())
                .from(transactionFilterStep.transactionSenderCstarMasterStep(this.hpanConnectorService))
                .on("*").to(transactionFilterStep.transactionSenderFtpMasterStep(this.sftpConnectorService))
                .on("*").to(fileManagementTask())
                .build();
    }

    @Bean
    public JobListener jobListener() {
        return new JobListener();
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
    public Step saltRecoveryTask(StoreService storeService) {
        SaltRecoveryTasklet saltRecoveryTasklet = new SaltRecoveryTasklet();
        saltRecoveryTasklet.setHpanConnectorService(hpanConnectorService);
        saltRecoveryTasklet.setStoreService(storeService);
        saltRecoveryTasklet.setTaskletEnabled(saltRecoveryEnabled);
        return stepBuilderFactory.get("transaction-filter-salt-recovery-step")
                .tasklet(saltRecoveryTasklet).build();
    }

    @Bean
    public Step pagopaPublicKeyRecoveryTask(StoreService storeService) {
        PagopaPublicKeyRecoveryTasklet pagopaPublicKeyRecoveryTasklet = new PagopaPublicKeyRecoveryTasklet();
        pagopaPublicKeyRecoveryTasklet.setHpanConnectorService(hpanConnectorService);
        pagopaPublicKeyRecoveryTasklet.setStoreService(storeService);
        pagopaPublicKeyRecoveryTasklet.setTaskletEnabled(pagopaPublicKeyRecoveryEnabled);
        return stepBuilderFactory.get("transaction-filter-public-key-recovery-step")
                .tasklet(pagopaPublicKeyRecoveryTasklet).build();
    }

    /**
     * @return step instance based on the {@link FileManagementTasklet} to be used for
     * file archival at the end of the reading process
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

    public StoreService batchStoreService() {
        return beanFactory.getBean(StoreService.class);
    }

}
