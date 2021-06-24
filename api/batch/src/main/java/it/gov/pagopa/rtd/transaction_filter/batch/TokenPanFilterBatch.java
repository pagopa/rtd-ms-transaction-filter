package it.gov.pagopa.rtd.transaction_filter.batch;

import it.gov.pagopa.rtd.transaction_filter.batch.step.*;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.JobListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.*;
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
import org.springframework.batch.core.job.builder.FlowBuilder;
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
import java.nio.file.Files;
import java.util.Date;

/**
 * Configuration of a scheduled batch job to read and decrypt .pgp files containing pan lists
 * (possibly recovered from a remote service), and .csv files to be processed in instances of Transaction class,
 * to be filtered by checking transaction pan (eventually hashed with a salt remotely recovered).
 * The output files can be encrypted with a public PGP key, and sent through an SFTP channel
 */

@Configuration
@Data
@PropertySource("classpath:config/tokenPanFilterBatch.properties")
@Import({TokenPanFilterStep.class, BinReaderStep.class, TokenPanFilterStep.class})
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class TokenPanFilterBatch {

    private final TokenPanFilterStep tokenPanFilterStep;
    private final TokenPanReaderStep tokenPanReaderStep;
    private final BinReaderStep binReaderStep;
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final BeanFactory beanFactory;
    private final TokenConnectorService tokenConnectorService;
    private final SftpConnectorService sftpConnectorService;

    @Value("${batchConfiguration.TokenPanFilterBatch.isolationForCreate}")
    private String isolationForCreate;
    @Value("${batchConfiguration.TokenPanFilterBatch.successArchivePath}")
    private String successArchivePath;
    @Value("${batchConfiguration.TokenPanFilterBatch.errorArchivePath}")
    private String errorArchivePath;
    @Value("${batchConfiguration.TokenPanFilterBatch.tablePrefix}")
    private String tablePrefix;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanListRecovery.directoryPath}")
    private String tokenPanListDirectory;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanListRecovery.filePattern}")
    private String tokenPanListRecoveryFilePattern;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanListRecovery.filename}")
    private String hpanListFilename;
    @Value("${batchConfiguration.TokenPanFilterBatch.binListRecovery.directoryPath}")
    private String binListDirectory;
    @Value("${batchConfiguration.TokenPanFilterBatch.binListRecovery.filePattern}")
    private String binListRecoveryFilePattern;
    @Value("${batchConfiguration.TokenPanFilterBatch.binListRecovery.filename}")
    private String binListFilename;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.deleteProcessedFiles}")
    private Boolean deleteProcessedFiles;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.deleteOutputFiles}")
    private String deleteOutputFiles;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.manageHpanOnSuccess}")
    private String manageHpanOnSuccess;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanListRecovery.enabled}")
    private Boolean tokenPanListRecoveryEnabled;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanListRecovery.dailyRemoval.enabled}")
    private Boolean tokenPanListDailyRemovalEnabled;
    @Value("${batchConfiguration.TokenPanFilterBatch.binListRecovery.enabled}")
    private Boolean binListRecoveryEnabled;
    @Value("${batchConfiguration.TokenPanFilterBatch.binListRecovery.dailyRemoval.enabled}")
    private Boolean binListDailyRemovalEnabled;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanList.numberPerFile}")
    private Long numberPerFile;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.tokenPanValidationEnabled}")
    private Boolean tokenPanValidationEnabled;

    private String salt = "";
    private DataSource dataSource;
    private TokenPanStoreService tokenPanStoreService;
    private BinStoreService binStoreService;
    private TransactionWriterService transactionWriterService;
    private WriterTrackerService writerTrackerService;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public void transactionWriterService() {
        transactionWriterService = beanFactory.getBean(TransactionWriterServiceImpl.class);
    }

    public void closeChannels() {
        transactionWriterService.closeAll();
    }

    public void createTokenPanStoreService(String workingTokenPanDirectory) {
        this.tokenPanStoreService = batchTokenPanStoreService(workingTokenPanDirectory);
        this.tokenPanStoreService.storeSalt(salt);
    }

    public void createBinStoreService(String workingBinDirectory) {
        this.binStoreService = batchBinStoreService(workingBinDirectory);
    }

    public void clearTokenPanStoreService() {
        tokenPanStoreService.clearAll();
    }

    public void clearBinStoreService() {
        binStoreService.clearAll();
    }

    public void clearStoreSet() {
        binStoreService.clearStoreSet();
        tokenPanStoreService.clearStoreSet();
    }

    public void createWriterTrackerService() {
        this.writerTrackerService = writerTrackerService();
    }

    public void clearWriterTrackerService() {
        writerTrackerService.clearAll();
    }

    public Boolean getTokenPanValidationEnabled() {
        return this.tokenPanValidationEnabled;
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
        String transactionsPath = tokenPanFilterStep.getTransactionDirectoryPath();

        String innerOutputPath = Files.createTempDirectory("tempTokenFolder").toFile().getAbsolutePath();
        innerOutputPath = "file:/".concat(innerOutputPath.charAt(0) == '/' ?
                innerOutputPath.replaceFirst("/","") : innerOutputPath);

        String tempOutputPath = Files.createTempDirectory("tempTokenPanFolder").toFile().getAbsolutePath();
        tempOutputPath = "file:/".concat(tempOutputPath.charAt(0) == '/' ?
                tempOutputPath.replaceFirst("/","") : tempOutputPath);

        String workingBinDirectory =    Files.createTempDirectory("workingBinFolder").toFile().getAbsolutePath();
        workingBinDirectory = "file:/".concat(workingBinDirectory.charAt(0) == '/' ?
                workingBinDirectory.replaceFirst("/","") : workingBinDirectory);

        String workingTokenPanDirectory =  Files.createTempDirectory("workingTokenPanFolder").toFile().getAbsolutePath();
        workingTokenPanDirectory = "file:/".concat(workingTokenPanDirectory.charAt(0) == '/' ?
                workingTokenPanDirectory.replaceFirst("/","") : workingTokenPanDirectory);

        Resource[] transactionResources = resolver.getResources(transactionsPath);

        String tokenPanPath = tokenPanReaderStep.getTokenPanDirectoryPath();
        Resource[] tokenPanResources = resolver.getResources(tokenPanPath);

        JobExecution execution = null;

        /** The jobLauncher run method is called only if, based on the configured properties, a matching transaction
        resource is found, and either the remote pan list recovery is enabled, or a pan list file is available locally
        on the configured path
         */
        if (transactionResources.length > 0 &&
                (getTokenPanListRecoveryEnabled() || tokenPanResources.length>0)) {

            log.info("Found {}. Starting filtering process",
                    transactionResources.length + (transactionResources.length > 1 ? "resources" : "resource")
            );

            if (transactionWriterService == null) {
                transactionWriterService();
            }
            createTokenPanStoreService(workingTokenPanDirectory);
            createBinStoreService(workingBinDirectory);
            createWriterTrackerService();

//            Resource[] tokenPanResourcesToDelete = resolver.getResources(
//                    tokenPanReaderStep.getTokenPanWorkerDirectoryPath());
//            for (Resource resource : tokenPanResourcesToDelete) {
//                FileUtils.forceDelete(resource.getFile());
//            }
            Resource[] tempTokenPanResourcesToDelete = resolver.getResources(
                    workingTokenPanDirectory.concat("/*.csv"));
            for (Resource resource : tempTokenPanResourcesToDelete) {
                FileUtils.forceDelete(resource.getFile());
            }
            Resource[] tempTransactionToDelete = resolver.getResources(
                    innerOutputPath.concat("/current/*.csv"));
            for (Resource resource : tempTransactionToDelete) {
                FileUtils.forceDelete(resource.getFile());
            }

            execution = tokenJobLauncher().run(tokenJob(),
                    new JobParametersBuilder()
                            .addDate("startDateTime", startDate)
                            .toJobParameters());

            Resource[] tokenPanWorkerResources = resolver.getResources(
                    workingTokenPanDirectory.concat("/*.csv"));

            Resource[] binWorkerResources = resolver.getResources(
                    workingBinDirectory.concat("/*.csv"));

            Integer binFilesCounter = 0;

            int binWorkerSize = binWorkerResources.length;

            while (!execution.getExitStatus().equals(ExitStatus.FAILED)
                    && binFilesCounter < binWorkerSize) {

                transactionResources = resolver.getResources(
                        binFilesCounter == 0 ?
                                transactionsPath :
                                innerOutputPath.concat("/*.csv"));

                Resource parResource = binWorkerResources[binFilesCounter];
                String tempData = workingBinDirectory.concat("/current");
                String file = parResource.getFile().getAbsolutePath();
                file = file.replaceAll("\\\\", "/");
                String[] filename = file.split("/");
                String[] filenameArr = filename[filename.length-1].split("\\.");
                filenameArr[filenameArr.length-1] = "csv";
                tempData = resolver.getResources(tempData)[0].getFile().getAbsolutePath();
                File destFile = FileUtils.getFile(tempData + "/" + String.join(".", filenameArr));
                FileUtils.moveFile(FileUtils.getFile(parResource.getFile()), destFile);
                binFilesCounter = binFilesCounter + 1;

                Boolean lastSection = binFilesCounter.equals(binWorkerSize);

                clearStoreSet();

                for (Resource transactionResource : transactionResources) {
                    tempData = innerOutputPath.concat("/current");
                    file = transactionResource.getFile().getAbsolutePath();
                    file = file.replaceAll("\\\\", "/");
                    filename = file.split("/");
                    filenameArr = filename[filename.length-1].split("\\.");
                    filenameArr[filenameArr.length-1] = "csv";
                    tempData = resolver.getResources(tempData)[0].getFile().getAbsolutePath();
                    destFile = FileUtils.getFile(tempData + "/" + String.join(".", filenameArr));
                    FileUtils.moveFile(FileUtils.getFile(transactionResource.getFile()), destFile);
                }

                Date innerStartDate = new Date();

                execution = tokenJobLauncher().run(tokenjobInner(),
                        new JobParametersBuilder()
                                .addDate("startDateTime", innerStartDate)
                                .addString("lastSection", String.valueOf(lastSection))
                                .addString("isBin",
                                        String.valueOf(true))
                                .addString("innerOutputPath",innerOutputPath)
                                .addString("temporaryOutputPath", tempOutputPath)
                                .addString("workingBinDirectory",workingBinDirectory)
                                .addString("workingTokenPanDirectory",workingTokenPanDirectory)
                                .addString("firstSection", String.valueOf(binFilesCounter.equals(1)))
                                .toJobParameters());
            }

            Integer tokenPanFilesCounter = 0;

            int tokenPanWorkerSize = tokenPanWorkerResources.length;

            while (!execution.getExitStatus().equals(ExitStatus.FAILED) &&
                    tokenPanFilesCounter < tokenPanWorkerSize) {

                transactionResources = resolver.getResources(
                        tokenPanFilesCounter == 0 ?
                                tempOutputPath.concat("/*.csv") :
                                innerOutputPath.concat("/*.csv"));

                Resource tokenPanResource = tokenPanWorkerResources[tokenPanFilesCounter];
                String tempData = workingTokenPanDirectory.concat("/current");
                String file = tokenPanResource.getFile().getAbsolutePath();
                file = file.replaceAll("\\\\", "/");
                String[] filename = file.split("/");
                String[] filenameArr = filename[filename.length-1].split("\\.");
                filenameArr[filenameArr.length-1] = "csv";
                tempData = resolver.getResources(tempData)[0].getFile().getAbsolutePath();
                File destFile = FileUtils.getFile(tempData + "/" + String.join(".", filenameArr));
                FileUtils.moveFile(FileUtils.getFile(tokenPanResource.getFile()), destFile);
                tokenPanFilesCounter = tokenPanFilesCounter + 1;

                clearStoreSet();

                for (Resource transactionResource : transactionResources) {
                    tempData = innerOutputPath.concat("/current");
                    file = transactionResource.getFile().getAbsolutePath();
                    file = file.replaceAll("\\\\", "/");
                    filename = file.split("/");
                    filenameArr = filename[filename.length-1].split("\\.");
                    filenameArr[filenameArr.length-1] = "csv";
                    tempData = resolver.getResources(tempData)[0].getFile().getAbsolutePath();
                    destFile = FileUtils.getFile(tempData + "/" + String.join(".", filenameArr));
                    FileUtils.moveFile(FileUtils.getFile(transactionResource.getFile()), destFile);
                }

                Date innerStartDate = new Date();

                Boolean lastSection = tokenPanFilesCounter.equals(tokenPanWorkerSize);
                execution = tokenJobLauncher().run(tokenPanBinJobInner(),
                        new JobParametersBuilder()
                                .addDate("startDateTime", innerStartDate)
                                .addString("lastSection",
                                        String.valueOf(lastSection))
                                .addString("isBin",
                                        String.valueOf(false))
                                .addString("innerOutputPath",innerOutputPath)
                                .addString("temporaryOutputPath", tempOutputPath)
                                .addString("workingBinDirectory",workingBinDirectory)
                                .addString("workingTokenPanDirectory",workingTokenPanDirectory)
                                .addString("firstSection", "false")
                                .toJobParameters());

                if (lastSection) {
                    for (Resource transactionResource : transactionResources) {
                        if (transactionResource.getFile().exists()) {
                            FileUtils.forceDelete(transactionResource.getFile());
                        }
                    }
                }

            }

            closeChannels();
            clearTokenPanStoreService();
            clearBinStoreService();
            clearWriterTrackerService();

        } else {
            if (transactionResources.length == 0) {
                log.info("No transaction file has been found on configured path: {}", transactionsPath);
            }
            if (!getTokenPanListRecoveryEnabled() && tokenPanResources.length==0) {
                log.info("No tokenPan file has been found on configured path: {}", tokenPanPath);
            }
        }

        return execution;

    }

    /**
     *
     * @return configured instance of TransactionManager
     */
    @Bean
    public PlatformTransactionManager tokenTransactionManager() {
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
    public JobRepository tokenJobRepository() throws Exception {
            JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
            jobRepositoryFactoryBean.setTransactionManager(tokenTransactionManager());
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
    public JobLauncher tokenJobLauncher() throws Exception {
        SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
        simpleJobLauncher.setJobRepository(tokenJobRepository());
        return simpleJobLauncher;
    }

    /**
     *
     * @return instance of a job for transaction processing
     */
    @SneakyThrows
    @Bean
    public Job tokenJob() {
        return tokenJobBuilder().build();
    }

    /**
     *
     * @return instance of a job for transaction processing
     */
    @SneakyThrows
    @Bean
    public Job tokenjobInner() {
        return tokenInnerJobBuilder().build();
    }

    /**
     *
     * @return instance of a job for transaction processing
     */
    @SneakyThrows
    @Bean
    public Job tokenPanBinJobInner() {
        return tokenPanInnerJobBuilder().build();
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
    public FlowJobBuilder tokenJobBuilder() {

       FlowBuilder.TransitionBuilder transitionBuilder = jobBuilderFactory.get("token-filter-job")
                .repository(tokenJobRepository())
                .listener(jobListener())
                .start(tokenPanListRecoveryTask())
                .on("FAILED").end()
                .from(tokenPanListRecoveryTask()).on("*").to(binListRecoveryTask())
                .on("FAILED").end()
                .from(binListRecoveryTask()).on("*");

                transitionBuilder = transitionBuilder.to(
                        binReaderStep.binRecoveryMasterStep(
                                this.binStoreService, this.writerTrackerService))
                        .on("FAILED").to(fileTokenManagementTask())
                        .from(binReaderStep.binRecoveryMasterStep(
                                this.binStoreService, this.writerTrackerService))
                        .on("*");


                transitionBuilder = transitionBuilder.to(
                        tokenPanReaderStep.enrolledTokenPanRecoveryMasterStep(
                                this.tokenPanStoreService, this.writerTrackerService))
                        .on("*");

                return (FlowJobBuilder) transitionBuilder.to(fileTokenManagementTask()).build();
    }

    @SneakyThrows
    public FlowJobBuilder tokenInnerJobBuilder() {

        return jobBuilderFactory.get("token-inner-bin-filter-job")
                .repository(tokenJobRepository())
                .listener(jobListener())
                .start(binReaderStep.binStoreRecoveryMasterStep(
                        this.binStoreService, this.writerTrackerService))
                .on("FAILED").to(innerTokenPanFileManagementTask())
                .from(binReaderStep.binStoreRecoveryMasterStep(
                        this.binStoreService, this.writerTrackerService))
                .on("*").to(tokenPanFilterStep.tokenPanBinFilterMasterStep(
                        this.binStoreService,this.transactionWriterService))
                .from(tokenPanFilterStep.tokenPanBinFilterMasterStep(
                        this.binStoreService,this.transactionWriterService))
                .on("FAILED").to(innerTokenPanFileManagementTask())
                .from(tokenPanFilterStep.tokenPanBinFilterMasterStep(
                        this.binStoreService,this.transactionWriterService))
                .on("*").to(tokenPanFilterStep.tokenSenderMasterStep(
                        this.sftpConnectorService))
                .on("*").to(innerTokenPanFileManagementTask())
                .build();
    }

    @SneakyThrows
    public FlowJobBuilder tokenPanInnerJobBuilder() {

        return jobBuilderFactory.get("token-inner-tokenpan-filter-job")
                .repository(tokenJobRepository())
                .listener(jobListener())
                .start(tokenPanReaderStep.enrolledTokenPanStoreRecoveryMasterStep(
                        this.tokenPanStoreService, this.writerTrackerService))
                .on("FAILED").to(innerTokenPanFileManagementTask())
                .from(tokenPanReaderStep.enrolledTokenPanStoreRecoveryMasterStep(
                        this.tokenPanStoreService, this.writerTrackerService))
                .on("*").to(tokenPanFilterStep.tokenPanFilterMasterStep(
                        this.tokenPanStoreService,this.transactionWriterService))
                .from(tokenPanFilterStep.tokenPanFilterMasterStep(
                        this.tokenPanStoreService,this.transactionWriterService))
                .on("FAILED").to(innerTokenPanFileManagementTask())
                .from(tokenPanFilterStep.tokenPanFilterMasterStep(
                        this.tokenPanStoreService,this.transactionWriterService))
                .on("*").to(tokenPanFilterStep.tokenSenderMasterStep(
                        this.sftpConnectorService))
                .on("*").to(innerTokenPanFileManagementTask())
                .build();
    }

    @Bean
    public JobListener jobListener() {
        return new JobListener();
    }

    @Bean
    public Step tokenPanListRecoveryTask() {
        TokenListRecoveryTasklet tokenListRecoveryTasklet = new TokenListRecoveryTasklet();
        tokenListRecoveryTasklet.setTokenListDirectory(tokenPanListDirectory);
        tokenListRecoveryTasklet.setTokenConnectorService(tokenConnectorService);
        tokenListRecoveryTasklet.setFileName(hpanListFilename);
        tokenListRecoveryTasklet.setTokenFilePattern(tokenPanListRecoveryFilePattern);
        tokenListRecoveryTasklet.setDailyRemovalTaskletEnabled(tokenPanListDailyRemovalEnabled);
        tokenListRecoveryTasklet.setRecoveryTaskletEnabled(tokenPanListRecoveryEnabled);
        return stepBuilderFactory
                .get("token-filter-token-list-recovery-step")
                .tasklet(tokenListRecoveryTasklet).build();
    }

    @Bean
    public Step binListRecoveryTask() {
        BinListRecoveryTasklet binListRecoveryTasklet = new BinListRecoveryTasklet();
        binListRecoveryTasklet.setBinListDirectory(binListDirectory);
        binListRecoveryTasklet.setTokenConnectorService(tokenConnectorService);
        binListRecoveryTasklet.setFileName(binListFilename);
        binListRecoveryTasklet.setBinFilePattern(binListRecoveryFilePattern);
        binListRecoveryTasklet.setDailyRemovalTaskletEnabled(binListDailyRemovalEnabled);
        binListRecoveryTasklet.setRecoveryTaskletEnabled(binListRecoveryEnabled);
        return stepBuilderFactory
                .get("token-filter-bin-list-recovery-step")
                .tasklet(binListRecoveryTasklet).build();
    }


    /**
     * @return step instance based on the {@link TokenFileManagementTasklet} to be used for
     * file archival at the end of the reading process
     */
    @SneakyThrows
    @Bean
    public Step fileTokenManagementTask() {
        TokenFileManagementTasklet fileManagementTasklet = new TokenFileManagementTasklet();
        fileManagementTasklet.setSuccessPath(successArchivePath);
        fileManagementTasklet.setErrorPath(errorArchivePath);
        fileManagementTasklet.setTokenPanDirectory(tokenPanReaderStep.getTokenPanDirectoryPath());
        fileManagementTasklet.setBinDirectory(binReaderStep.getBinDirectoryPath());
        fileManagementTasklet.setOutputDirectory(tokenPanFilterStep.getOutputDirectoryPath());
        fileManagementTasklet.setDeleteProcessedFiles(deleteProcessedFiles);
        fileManagementTasklet.setDeleteOutputFiles(deleteOutputFiles);
        fileManagementTasklet.setManageBinOnSuccess(manageHpanOnSuccess);
        return stepBuilderFactory.get("transaction-filter-file-management-step")
                .tasklet(fileManagementTasklet).build();
    }

    /**
     * @return step instance based on the {@link TokenFileManagementTasklet} to be used for
     * file archival at the end of the reading process
     */
    @SneakyThrows
    @Bean
    public Step innerTokenPanFileManagementTask() {
        return stepBuilderFactory.get("token-inner-filter-file-management-step")
                .tasklet(innerTokenPanFileManagementTasklet(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null))
                .build();
    }

    @Bean
    @StepScope
    public InnerTokenPanFileManagementTasklet innerTokenPanFileManagementTasklet(
            @Value("#{jobParameters['firstSection']}") Boolean firstSection,
            @Value("#{jobParameters['lastSection']}") Boolean lastSection,
            @Value("#{jobParameters['isBin']}") Boolean isBin,
            @Value("#{jobParameters['innerOutputPath']}") String innerOutputPath,
            @Value("#{jobParameters['temporaryOutputPath']}") String tempOutputPath,
            @Value("#{jobParameters['workingBinDirectory']}") String workingBinDirectory,
            @Value("#{jobParameters['workingTokenPanDirectory']}") String workingTokenPanDirectory) {
        InnerTokenPanFileManagementTasklet fileManagementTasklet = new InnerTokenPanFileManagementTasklet();
        fileManagementTasklet.setSuccessPath(successArchivePath);
        fileManagementTasklet.setErrorPath(errorArchivePath);
        fileManagementTasklet.setBinDirectory(binReaderStep.getBinDirectoryPath());
        fileManagementTasklet.setTempBinDirectory(workingBinDirectory);
        fileManagementTasklet.setTokenPanDirectory(tokenPanReaderStep.getTokenPanDirectoryPath());
        fileManagementTasklet.setTempTokenPanDirectory(workingTokenPanDirectory);
        fileManagementTasklet.setOutputDirectory(tokenPanFilterStep.getOutputDirectoryPath());
        fileManagementTasklet.setInnerOutputDirectory(innerOutputPath);
        fileManagementTasklet.setTemporaryOutputPath(tempOutputPath);
        fileManagementTasklet.setDeleteProcessedFiles(deleteProcessedFiles);
        fileManagementTasklet.setDeleteOutputFiles(deleteOutputFiles);
        fileManagementTasklet.setManageBinOnSuccess(manageHpanOnSuccess);
        fileManagementTasklet.setFirstSection(firstSection);
        fileManagementTasklet.setLastSection(lastSection);
        fileManagementTasklet.setIsBin(isBin);
        return fileManagementTasklet;
    }

    public TokenPanStoreService batchTokenPanStoreService(String workingTokenPanDirectory) {
        TokenPanStoreService tokenPanStoreService = beanFactory.getBean(TokenPanStoreService.class);
        tokenPanStoreService.setNumberPerFile(numberPerFile);
        tokenPanStoreService.setWorkingTokenPANDirectory(workingTokenPanDirectory);
        tokenPanStoreService.setCurrentNumberOfData(0L);
        return tokenPanStoreService;
    }

    public BinStoreService batchBinStoreService(String workingBinDirectory) {
        BinStoreService binStoreService = beanFactory.getBean(BinStoreService.class);
        binStoreService.setNumberPerFile(numberPerFile);
        binStoreService.setWorkingBinDirectory(workingBinDirectory);
        binStoreService.setCurrentNumberOfData(0L);
        return binStoreService;
    }

    public WriterTrackerService writerTrackerService() {
        return beanFactory.getBean(WriterTrackerService.class);
    }

}
