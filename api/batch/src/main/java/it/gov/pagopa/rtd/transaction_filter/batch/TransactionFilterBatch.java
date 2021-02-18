package it.gov.pagopa.rtd.transaction_filter.batch;

import it.gov.pagopa.rtd.transaction_filter.batch.step.PanReaderStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.TransactionFilterStep;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.JobListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.FileManagementTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.HpanListRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.InnerFileManagementTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.SaltRecoveryTasklet;
import it.gov.pagopa.rtd.transaction_filter.service.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
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
import java.io.File;
import java.util.Date;

/**
 * Configuration of a scheduled batch job to read and decrypt .pgp files containing pan lists
 * (possibly recovered from a remote service), and .csv files to be processed in instances of Transaction class,
 * to be filtered by checking transaction pan (eventually hashed with a salt remotely recovered).
 * The output files can be encrypted with a public PGP key, and sent through an SFTP channel
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
    @Value("${batchConfiguration.TransactionFilterBatch.hpanList.numberPerFile}")
    private Long numberPerFile;
    @Value("${batchConfiguration.TransactionFilterBatch.hpanList.workingHpanDirectory}")
    private String workingHpanDirectory;

    private DataSource dataSource;
    private HpanStoreService hpanStoreService;
    private TransactionWriterService transactionWriterService;
    private WriterTrackerService writerTrackerService;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public void transactionWriterService() {
        transactionWriterService = beanFactory.getBean(TransactionWriterServiceImpl.class);
    }

    public void closeChannels() {
        transactionWriterService.closeAll();
    }

    public void createHpanStoreService() {
        this.hpanStoreService = batchHpanStoreService();
    }

    public void clearHpanStoreService() {
        hpanStoreService.clearAll();
    }

    public void clearStoreSet() {
        hpanStoreService.clearStoreSet();
    }


    public void createWriterTrackerService() {
        this.writerTrackerService = writerTrackerService();
    }

    public void clearWriterTrackerService() {
        writerTrackerService.clearAll();
    }

    /**
     *
     * @return Method to start the execution of the transaction filter job
     * @param startDate starting date for the batch job execution
     * @throws java.io.IOException
     * @throws  java.lang.Exception
     */
    @SneakyThrows
    public JobExecution executeBatchJob(Date startDate) {
        String transactionsPath = transactionFilterStep.getTransactionDirectoryPath();
        String innerOutputPath = transactionFilterStep.getInnerOutputDirectoryPath();
        Resource[] transactionResources = resolver.getResources(transactionsPath);

        String hpanPath = panReaderStep.getHpanDirectoryPath();
        Resource[] hpanResources = resolver.getResources(hpanPath);

        JobExecution execution = null;

        /** The jobLauncher run method is called only if, based on the configured properties, a matching transaction
        resource is found, and either the remote pan list recovery is enabled, or a pan list file is available locally
        on the configured path
         */
        if (transactionResources.length > 0 &&
                (getHpanListRecoveryEnabled() || hpanResources.length>0)) {

            log.info("Found {}. Starting filtering process",
                    transactionResources.length + (transactionResources.length > 1 ? "resources" : "resource")
            );

            if (transactionWriterService == null) {
                transactionWriterService();
            }
            createHpanStoreService();
            createWriterTrackerService();

            execution = jobLauncher().run(job(),
                    new JobParametersBuilder()
                            .addDate("startDateTime", startDate)
                            .toJobParameters());

            Resource[] hpanWorkerResources = resolver.getResources(
                    workingHpanDirectory.concat("/*.csv"));

            Integer hpanWorkerSize = hpanWorkerResources.length;
            Integer hpanFilesCounter = 0;

            for (Resource resource : hpanWorkerResources) {

                hpanFilesCounter = hpanFilesCounter+1;
                clearStoreSet();

                String tempData = workingHpanDirectory.concat("/current");
                String file = resource.getFile().getAbsolutePath();
                file = file.replaceAll("\\\\", "/");
                String[] filename = file.split("/");
                tempData = resolver.getResources(tempData)[0].getFile().getAbsolutePath();
                File destFile = FileUtils.getFile(tempData + "/" + filename[filename.length - 1]);
                FileUtils.moveFile(FileUtils.getFile(resource.getFile()), destFile);

                transactionResources = resolver.getResources(transactionsPath);
                for (Resource transactionResource : transactionResources) {
                    tempData = innerOutputPath.concat("/current");
                    file = transactionResource.getFile().getAbsolutePath();
                    file = file.replaceAll("\\\\", "/");
                    filename = file.split("/");
                    tempData = resolver.getResources(tempData)[0].getFile().getAbsolutePath();
                    destFile = FileUtils.getFile(tempData + "/" + filename[filename.length - 1]);
                    FileUtils.moveFile(FileUtils.getFile(transactionResource.getFile()), destFile);
                }

                Date innerStartDate = new Date();

                Boolean lastSection = hpanFilesCounter.equals(hpanWorkerSize);
                execution = jobLauncher().run(jobInner(),
                        new JobParametersBuilder()
                                .addDate("startDateTime", innerStartDate)
                                .addString("lastSection",
                                        String.valueOf(lastSection))
                                .addString("firstSection",
                                        String.valueOf(hpanFilesCounter.equals(1)))
                                .toJobParameters());

                if (lastSection) {
                    for (Resource transactionResource : transactionResources) {
                        FileUtils.forceDelete(transactionResource.getFile());
                    }
                }

            }

            closeChannels();
            clearHpanStoreService();
            clearWriterTrackerService();

        } else {
            if (transactionResources.length == 0) {
                log.info("No transaction file has been found on configured path: {}", transactionsPath);
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
     * @return instance of a job for transaction processing
     */
    @SneakyThrows
    @Bean
    public Job jobInner() {
        return transactionInnerJobBuilder().build();
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
     * @throws java.lang.Exception
     * @return Instance of {@link FlowJobBuilder}, with the configured steps executed
     * for the pan/transaction processing
     */
    @SneakyThrows
    public FlowJobBuilder transactionJobBuilder() {

        /**
         * The flow is defined with the followings steps:
         * 1) Attempts panlist recovery, if enabled. In case of a failure in the execution, the process is stopped.
         * 2) Attempts salt recovery, if enabled. In case of a failure in the execution, the process is stopped.
         *    Otherwise, the panList step is executed
         * 3) The panList step is executed, to store the .csv files including the list of active pans. If the step
         *    fails, the file archival tasklet is called, otherwise the transaction filter step is called.
         * 4) The transaction filter step checks the records with the stored pans, writing the matching records in
         *    the output file. If the process fails, the file management tasklet is called,
         *    otherwise the transaction sender step si called.
         * 5) Attempts sending the output files through an sftp channel, if enabled. The file management tasklet
         *    is always called, after the step
         * 6) After all the possible file management executions, the process is stopped
         */

        return jobBuilderFactory.get("transaction-filter-job")
                .repository(getJobRepository())
                .listener(jobListener())
                .start(hpanListRecoveryTask())
                .on("FAILED").end()
                .from(hpanListRecoveryTask()).on("*").to(saltRecoveryTask(this.hpanStoreService))
                .on("FAILED").end()
                .from(saltRecoveryTask(this.hpanStoreService)).on("*")
                .to(panReaderStep.hpanRecoveryMasterStep(this.hpanStoreService, this.writerTrackerService))
                .on("*").to(fileManagementTask())
                .build();
    }

    @SneakyThrows
    public FlowJobBuilder transactionInnerJobBuilder() {

        return jobBuilderFactory.get("transaction-inner-filter-job")
                .repository(getJobRepository())
                .listener(jobListener())
                .start(panReaderStep.hpanStoreRecoveryMasterStep(this.hpanStoreService, this.writerTrackerService))
                .on("FAILED").to(innerFileManagementTask())
                .from(panReaderStep.hpanStoreRecoveryMasterStep(this.hpanStoreService, this.writerTrackerService))
                .on("*").to(transactionFilterStep.transactionFilterMasterStep(this.hpanStoreService,this.transactionWriterService))
                .from(transactionFilterStep.transactionFilterMasterStep(this.hpanStoreService,this.transactionWriterService))
                .on("FAILED").to(innerFileManagementTask())
                .from(transactionFilterStep.transactionFilterMasterStep(this.hpanStoreService,this.transactionWriterService))
                .on("*").to(transactionFilterStep.transactionSenderMasterStep(
                        this.sftpConnectorService))
                .on("*").to(innerFileManagementTask())
                .build();
    }

    @Bean
    public JobListener jobListener() {
        JobListener jobListener = new JobListener();
        return jobListener;
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

    /**
     * @return step instance based on the {@link FileManagementTasklet} to be used for
     * file archival at the end of the reading process
     */
    @SneakyThrows
    @Bean
    public Step innerFileManagementTask() {
        return stepBuilderFactory.get("transaction-inner-filter-file-management-step")
                .tasklet(innerFileManagementTasklet(null)).build();
    }

    @Bean
    @StepScope
    public InnerFileManagementTasklet innerFileManagementTasklet(
            @Value("#{jobParameters['firstSection']}") Boolean firstSection) {
        InnerFileManagementTasklet fileManagementTasklet = new InnerFileManagementTasklet();
        fileManagementTasklet.setSuccessPath(successArchivePath);
        fileManagementTasklet.setErrorPath(errorArchivePath);
        fileManagementTasklet.setHpanDirectory(panReaderStep.getHpanWorkerDirectoryPath());
        fileManagementTasklet.setOutputDirectory(transactionFilterStep.getOutputDirectoryPath());
        fileManagementTasklet.setDeleteProcessedFiles(deleteProcessedFiles);
        fileManagementTasklet.setDeleteOutputFiles(deleteOutputFiles);
        fileManagementTasklet.setManageHpanOnSuccess(manageHpanOnSuccess);
        fileManagementTasklet.setFirstSection(firstSection);
        return fileManagementTasklet;
    }

    public HpanStoreService batchHpanStoreService() {
        HpanStoreService hpanStoreService = beanFactory.getBean(HpanStoreService.class);
        hpanStoreService.setNumberPerFile(numberPerFile);
        hpanStoreService.setWorkingHpanDirectory(workingHpanDirectory);
        hpanStoreService.setCurrentNumberOfData(0L);
        return hpanStoreService;
    }

    public WriterTrackerService writerTrackerService() {
        return beanFactory.getBean(WriterTrackerService.class);
    }




}
