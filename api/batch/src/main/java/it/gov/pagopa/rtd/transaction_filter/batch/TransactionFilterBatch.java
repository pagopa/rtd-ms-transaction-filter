package it.gov.pagopa.rtd.transaction_filter.batch;


import it.gov.pagopa.rtd.transaction_filter.batch.step.PanReaderStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.JobListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.AbiToFiscalCodeMapRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.EnforceSenderCodeUniquenessTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.FileManagementTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.HpanListRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.PagopaPublicKeyRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.PreventReprocessingFilenameAlreadySeenTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.PurgeAggregatesFromMemoryTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.SenderAdeAckFilesRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.SaltRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.SelectTargetInputFileTasklet;
import it.gov.pagopa.rtd.transaction_filter.connector.AbiToFiscalCodeRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SenderAdeAckRestClient;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
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
    private final HpanConnectorService hpanConnectorService;
    private final AbiToFiscalCodeRestClient abiToFiscalCodeRestClient;
    private final SenderAdeAckRestClient senderAdeAckRestClient;
    private final TransactionWriterService transactionWriterService;
    private final StoreService storeService;

    private static final String FAILED = "FAILED";

    @Value("${batchConfiguration.TransactionFilterBatch.isolationForCreate}")
    private String isolationForCreate;
    @Value("${batchConfiguration.TransactionFilterBatch.successArchivePath}")
    private String successArchivePath;
    @Value("${batchConfiguration.TransactionFilterBatch.errorArchivePath}")
    private String errorArchivePath;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.pendingDirectoryPath}")
    private String pendingArchivePath;
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
    @Value("${batchConfiguration.TransactionFilterBatch.abiToFiscalCodeMapRecovery.enabled}")
    private Boolean abiToFiscalCodeTaskletEnabled;
    @Value("${batchConfiguration.TransactionFilterBatch.senderAdeAckFilesRecovery.enabled}")
    private Boolean senderAdeAckFilesTaskletEnabled;
    @Value("${batchConfiguration.TransactionFilterBatch.senderAdeAckFilesRecovery.directoryPath}")
    private String senderAdeAckFilesDirectoryPath;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.transactionLogsPath}")
    private String logsDirectoryPath;

    private DataSource dataSource;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public void closeChannels() {
        transactionWriterService.closeAll();
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

            execution = jobLauncher().run(job(),
                    new JobParametersBuilder()
                            .addDate("startDateTime", startDate)
                            .toJobParameters());
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
                .on("*").to(selectTargetInputFileTask(this.storeService))
                .on(FAILED).end()
                .on("*").to(preventReprocessingFilenameAlreadySeenTask(this.storeService, this.transactionWriterService))
                .on(FAILED).end()
                .from(preventReprocessingFilenameAlreadySeenTask(this.storeService, this.transactionWriterService))
                .on("*").to(transactionFilterStep.transactionChecksumMasterStep(this.storeService))
                .on(FAILED).end()
                .from(transactionFilterStep.transactionChecksumMasterStep(this.storeService))
                .on("*").to(abiToFiscalCodeMapRecoveryTask(this.storeService))
                .on(FAILED).end()
                .from(abiToFiscalCodeMapRecoveryTask(this.storeService))
                .on("*").to(transactionFilterStep.transactionAggregationReaderMasterStep(this.storeService, this.transactionWriterService))
                .on(FAILED).to(fileManagementTask())
                .from(transactionFilterStep.transactionAggregationReaderMasterStep(this.storeService, this.transactionWriterService))
                .on("*").to(enforceSenderCodeUniquenessTask(this.storeService))
                .on(FAILED).end()
                .from(enforceSenderCodeUniquenessTask(this.storeService))
                .on("*").to(transactionFilterStep.transactionAggregationWriterMasterStep(this.storeService))
                .on(FAILED).to(fileManagementTask())
                .from(transactionFilterStep.transactionAggregationWriterMasterStep(this.storeService))
                .on("*").to(purgeAggregatesFromMemoryTask(this.storeService))
                .on(FAILED).to(fileManagementTask())
                .from(purgeAggregatesFromMemoryTask(this.storeService))
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
                .on("*").to(transactionFilterStep.transactionFilterMasterStep(this.storeService, this.transactionWriterService))
                .on(FAILED).to(fileManagementTask())
                .from(transactionFilterStep.transactionFilterMasterStep(this.storeService, this.transactionWriterService))
                .on("*").to(transactionFilterStep.transactionSenderRtdMasterStep(this.hpanConnectorService))
                .on(FAILED).to(fileManagementTask())
                .from(transactionFilterStep.transactionSenderRtdMasterStep(this.hpanConnectorService))
                .on("*").to(senderAdeAckFilesRecoveryTask())
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

    @Bean
    public Step purgeAggregatesFromMemoryTask(StoreService storeService) {
        PurgeAggregatesFromMemoryTasklet tasklet = new PurgeAggregatesFromMemoryTasklet();
        tasklet.setStoreService(storeService);
        return stepBuilderFactory.get("transaction-filter-purge-aggregates-from-memory-step")
            .tasklet(tasklet).build();
    }

    @Bean
    public Step selectTargetInputFileTask(StoreService storeService) {
        SelectTargetInputFileTasklet tasklet = new SelectTargetInputFileTasklet();
        tasklet.setStoreService(storeService);
        tasklet.setTransactionDirectoryPath(transactionFilterStep.getTransactionDirectoryPath());
        return stepBuilderFactory.get("transaction-filter-select-target-input-file-step")
            .tasklet(tasklet).build();
    }

    @Bean
    public Step enforceSenderCodeUniquenessTask(StoreService storeService) {
        EnforceSenderCodeUniquenessTasklet tasklet = new EnforceSenderCodeUniquenessTasklet();
        tasklet.setStoreService(storeService);
        return stepBuilderFactory.get("transaction-filter-enforce-uniqueness-sender-code-step")
            .tasklet(tasklet).build();
    }

    @Bean
    public Step preventReprocessingFilenameAlreadySeenTask(StoreService storeService, TransactionWriterService transactionWriterService) {
        PreventReprocessingFilenameAlreadySeenTasklet tasklet = new PreventReprocessingFilenameAlreadySeenTasklet();
        tasklet.setStoreService(storeService);
        tasklet.setTransactionWriterService(transactionWriterService);
        return stepBuilderFactory.get("prevent-reprocessing-filename-already-seen-step")
            .tasklet(tasklet).build();
    }

    @Bean
    public Step abiToFiscalCodeMapRecoveryTask(StoreService storeService) {
        AbiToFiscalCodeMapRecoveryTasklet tasklet = new AbiToFiscalCodeMapRecoveryTasklet(abiToFiscalCodeRestClient, storeService);
        tasklet.setTaskletEnabled(abiToFiscalCodeTaskletEnabled);
        return stepBuilderFactory
            .get("transaction-filter-abi-to-fiscalcode-recovery-step")
            .tasklet(tasklet).build();
    }

    @Bean
    public Step senderAdeAckFilesRecoveryTask() {
        SenderAdeAckFilesRecoveryTasklet tasklet = new SenderAdeAckFilesRecoveryTasklet(senderAdeAckRestClient);
        tasklet.setSenderAdeAckDirectory(senderAdeAckFilesDirectoryPath);
        tasklet.setTaskletEnabled(senderAdeAckFilesTaskletEnabled);

        return stepBuilderFactory
            .get("transaction-filter-sender-ade-ack-files-recovery-step")
            .tasklet(tasklet).build();
    }

    /**
     * @return step instance based on the {@link FileManagementTasklet} to be used for
     * file archival at the end of the reading process
     */
    @SneakyThrows
    @Bean
    public Step fileManagementTask() {
        FileManagementTasklet fileManagementTasklet = new FileManagementTasklet();
        fileManagementTasklet.setTransactionWriterService(transactionWriterService);
        fileManagementTasklet.setSuccessPath(successArchivePath);
        fileManagementTasklet.setUploadPendingPath(pendingArchivePath);
        fileManagementTasklet.setHpanDirectory(panReaderStep.getHpanDirectoryPath());
        fileManagementTasklet.setOutputDirectory(transactionFilterStep.getOutputDirectoryPath());
        fileManagementTasklet.setDeleteProcessedFiles(deleteProcessedFiles);
        fileManagementTasklet.setDeleteOutputFiles(deleteOutputFiles);
        fileManagementTasklet.setManageHpanOnSuccess(manageHpanOnSuccess);
        fileManagementTasklet.setLogsDirectory(logsDirectoryPath);
        return stepBuilderFactory.get("transaction-filter-file-management-step")
                .tasklet(fileManagementTasklet).build();
    }

}
