package it.gov.pagopa.rtd.transaction_filter.batch.step;

import it.gov.pagopa.rtd.transaction_filter.batch.config.BatchConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.mapper.InboundTokenPanFieldSetMapper;
import it.gov.pagopa.rtd.transaction_filter.batch.mapper.InboundTokenPanLineAwareMapper;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import it.gov.pagopa.rtd.transaction_filter.batch.step.classifier.InboundTokenPanClassifier;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.*;
import it.gov.pagopa.rtd.transaction_filter.batch.step.processor.InboundBinTokenPanItemProcessor;
import it.gov.pagopa.rtd.transaction_filter.batch.step.processor.InboundTokenPanItemProcessor;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.TokenPanFlatFileItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.TransactionSenderTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.PGPFlatFileItemWriter;
import it.gov.pagopa.rtd.transaction_filter.service.BinStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.SftpConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.TokenPanStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.*;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.validation.ConstraintViolationException;
import java.io.FileNotFoundException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

@Configuration
@DependsOn({"partitionerTaskExecutor","readerTaskExecutor"})
@RequiredArgsConstructor
@Data
@PropertySource("classpath:config/tokenFilterStep.properties")
public class TokenPanFilterStep {

    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.partitionerSize}")
    private Integer partitionerSize;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.chunkSize}")
    private Integer chunkSize;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.skipLimit}")
    private Integer skipLimit;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.transactionDirectoryPath}")
    private String transactionDirectoryPath;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.outputDirectoryPath}")
    private String outputDirectoryPath;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.publicKeyPath}")
    private String publicKeyPath;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.linesToSkip}")
    private Integer linesToSkip;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.timestampPattern}")
    private String timestampPattern;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.applyHashing}")
    private Boolean applyTrxHashing;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.applyEncrypt}")
    private Boolean applyEncrypt;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.sftp.localdirectory}")
    private String localdirectory;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanSender.enabled}")
    private Boolean tokenSenderEnabled;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.tokenPanLogsPath}")
    private String tokenPanLogsPath;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.enableAfterReadLogging}")
    private Boolean enableAfterReadLogging;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.enableOnReadErrorFileLogging}")
    private Boolean enableOnReadErrorFileLogging;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.enableOnReadErrorLogging}")
    private Boolean enableOnReadErrorLogging;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.enableAfterProcessLogging}")
    private Boolean enableAfterProcessLogging;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.enableAfterProcessFileLogging}")
    private Boolean enableAfterProcessFileLogging;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.enableOnProcessErrorFileLogging}")
    private Boolean enableOnProcessErrorFileLogging;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.enableOnProcessErrorLogging}")
    private Boolean enableOnProcessErrorLogging;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.enableAfterWriteLogging}")
    private Boolean enableAfterWriteLogging;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.enableOnWriteErrorFileLogging}")
    private Boolean enableOnWriteErrorFileLogging;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.enableOnWriteErrorLogging}")
    private Boolean enableOnWriteErrorLogging;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.loggingFrequency}")
    private Long loggingFrequency;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.readers.listener.writerPoolSize}")
    private Integer writerPoolSize;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.binValidationEnabled}")
    private Boolean binValidationEnabled;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.tokenPanValidationEnabled}")
    private Boolean tokenPanValidationEnabled;
    @Value("${batchConfiguration.TokenPanFilterBatch.tokenPanFilter.exemptedCircuitType}")
    private String exemptedCircuitType;

    private final BatchConfig batchConfig;
    private final StepBuilderFactory stepBuilderFactory;
    private ExecutorService writerExecutor;

    /**
     *
     * @return instance of the LineTokenizer to be used in the itemReader configured for the job
     */
    @Bean
    public LineTokenizer tokenPanLineTokenizer() {
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setDelimiter(";");
        delimitedLineTokenizer.setNames("token_pan", "circuit_type", "par");
        return delimitedLineTokenizer;
    }

    /**
     *
     * @return instance of the FieldSetMapper to be used in the itemReader configured for the job
     */
    @Bean
    public FieldSetMapper<InboundTokenPan> tokenPanFieldSetMapper() {
        return new InboundTokenPanFieldSetMapper();
    }

    /**
     *
     * @return instance of the LineMapper to be used in the itemReader configured for the job
     */
    public LineMapper<InboundTokenPan> tokenPanLineMapper(String fileName) {
        InboundTokenPanLineAwareMapper<InboundTokenPan> lineMapper =
                new InboundTokenPanLineAwareMapper<>();
        lineMapper.setTokenizer(tokenPanLineTokenizer());
        lineMapper.setFieldSetMapper(tokenPanFieldSetMapper());
        lineMapper.setFilename(fileName);
        return lineMapper;
    }

    /**
     *
     * @param file
     *          Late-Binding parameter to be used as the resource for the reader instance
     * @return instance of the itemReader to be used in the first step of the configured job
     */
    @SneakyThrows
    @Bean
    @StepScope
    public TokenPanFlatFileItemReader tokenPanItemReader(
            @Value("#{stepExecutionContext['fileName']}") String file) {
        TokenPanFlatFileItemReader flatFileItemReader = new TokenPanFlatFileItemReader();
        flatFileItemReader.setResource(new UrlResource(file));
        flatFileItemReader.setLineMapper(tokenPanLineMapper(file));
        flatFileItemReader.setLinesToSkip(linesToSkip);
        return flatFileItemReader;
    }

    /**
     *
     * @return instance of the LineTokenizer to be used in the itemReader configured for the job
     */
    @Bean
    public BeanWrapperFieldExtractor<InboundTokenPan> tokenPanWriterFieldExtractor() {
        BeanWrapperFieldExtractor<InboundTokenPan> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(new String[] {
                "tokenPan", "circuit_type", "par"});
        return extractor;
    }

    /**
     *
     * @return instance of the LineTokenizer to be used in the itemReader configured for the job
     */
    @Bean
    public LineAggregator<InboundTokenPan> tokenPanWriterAggregator() {
        DelimitedLineAggregator<InboundTokenPan> delimitedLineTokenizer = new DelimitedLineAggregator<>();
        delimitedLineTokenizer.setDelimiter(";");
        delimitedLineTokenizer.setFieldExtractor(tokenPanWriterFieldExtractor());
        return delimitedLineTokenizer;
    }

    /**
     *
     * @param file
     *          Late-Binding parameter to be used as the resource for the reader instance
     * @return instance of the itemReader to be used in the first step of the configured job
     */
    @SneakyThrows
    @Bean
    @StepScope
    public PGPFlatFileItemWriter<InboundTokenPan> tokenPanItemWriter(
            @Value("#{stepExecutionContext['fileName']}") String file,
            @Value("#{jobParameters['lastSection']}") Boolean lastSection) {
        PGPFlatFileItemWriter<InboundTokenPan> flatFileItemWriter =
                new PGPFlatFileItemWriter<>(publicKeyPath, applyEncrypt, lastSection);
        flatFileItemWriter.setLineAggregator(tokenPanWriterAggregator());
        flatFileItemWriter.setAppendAllowed(true);
        file = file.replaceAll("\\\\", "/");
        String[] filename = file.split("/");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        flatFileItemWriter.setResource(
                resolver.getResource(outputDirectoryPath.concat("/".concat(filename[filename.length-1]))));
        return flatFileItemWriter;
    }

    /**
     *
     * @param file
     *          Late-Binding parameter to be used as the resource for the reader instance
     * @return instance of the itemReader to be used in the first step of the configured job
     */
    @SneakyThrows
    @Bean
    @StepScope
    public FlatFileItemWriter<InboundTokenPan> tokenPanFilteredItemWriter(
            @Value("#{stepExecutionContext['fileName']}") String file,
            @Value("#{jobParameters['innerOutputPath']}") String innerOutputPath) {
        FlatFileItemWriter<InboundTokenPan> flatFileItemWriter = new FlatFileItemWriter<>();
        flatFileItemWriter.setLineAggregator(tokenPanWriterAggregator());
        flatFileItemWriter.setAppendAllowed(true);
        file = file.replaceAll("\\\\", "/");
        String[] filename = file.split("/");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        flatFileItemWriter.setResource(
                resolver.getResource(innerOutputPath.concat("/"
                        .concat(filename[filename.length-1]))));
        return flatFileItemWriter;
    }


    /**
     *
     * @return instance of the itemProcessor to be used in the first step of the configured job
     */
    @Bean
    @StepScope
    public InboundTokenPanItemProcessor tokenPanItemProcessor(
            TokenPanStoreService tokenPANStoreService,
            @Value("#{jobParameters['lastSection']}") Boolean lastSection) {
        InboundTokenPanItemProcessor inboundTokenPanItemProcessor =
                new InboundTokenPanItemProcessor(tokenPANStoreService, lastSection, binValidationEnabled);
        inboundTokenPanItemProcessor.setExemptedCircuitType(new ArrayList<>(
                Arrays.asList(exemptedCircuitType.split(","))));
        return inboundTokenPanItemProcessor;
    }

    /**
     *
     * @return instance of the itemProcessor to be used in the first step of the configured job
     */
    @Bean
    @StepScope
    public InboundBinTokenPanItemProcessor tokenPanBinItemProcessor(
            BinStoreService binStoreService,
            @Value("#{jobParameters['lastSection']}") Boolean lastSection) {
        InboundBinTokenPanItemProcessor inboundBinTokenPanItemProcessor =
                new InboundBinTokenPanItemProcessor(binStoreService, lastSection, binValidationEnabled);
        inboundBinTokenPanItemProcessor.setExemptedCircuitType(new ArrayList<>(
                Arrays.asList(exemptedCircuitType.split(","))));
        return inboundBinTokenPanItemProcessor;
    }

    /**
     *
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws Exception
     */
    @Bean
    @JobScope
    public Partitioner tokenPanFilterPartitioner(
            @Value("#{jobParameters['innerOutputPath']}") String innerOutputPath) throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(innerOutputPath.concat("/current/*.csv")));
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     *
     * @return master step to be used as the formal main step in the reading phase of the job,
     * partitioned for scalability on multiple file reading
     * @throws Exception
     */
    @Bean
    public Step tokenPanFilterMasterStep(TokenPanStoreService tokenPANStoreService,
                                         TransactionWriterService transactionWriterService)
            throws Exception {
        return stepBuilderFactory.get("token-filter-master-step").partitioner(
                tokenPanFilterWorkerStep(tokenPANStoreService, transactionWriterService))
                .partitioner("partition", tokenPanFilterPartitioner(null))
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step tokenPanFilterWorkerStep(
            TokenPanStoreService tokenPANStoreService,
            TransactionWriterService transactionWriterService)
            throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);
        return simpleWorkerStep(
                tokenPANStoreService, transactionWriterService, executionDate);
    }


    /**
     *
     * @return master step to be used as the formal main step in the reading phase of the job,
     * partitioned for scalability on multiple file reading
     * @throws Exception
     */
    @Bean
    public Step tokenPanBinFilterMasterStep(BinStoreService binStoreService,
                                         TransactionWriterService transactionWriterService)
            throws Exception {
        return stepBuilderFactory.get("token-filter-bin-master-step").partitioner(
                tokenPanBinFilterWorkerStep(binStoreService, transactionWriterService))
                .partitioner("partition", tokenPanFilterPartitioner(null))
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step tokenPanBinFilterWorkerStep(
            BinStoreService binStoreService,
            TransactionWriterService transactionWriterService)
            throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);
        return simpleBinWorkerStep(
                binStoreService, transactionWriterService, executionDate);
    }



    public Step simpleBinWorkerStep(BinStoreService binStoreService,
                                 TransactionWriterService transactionWriterService,
                                 String executionDate) throws Exception {
        return stepBuilderFactory.get("token-pan-bin-worker-step")
                .<InboundTokenPan, InboundTokenPan>chunk(chunkSize)
                .reader(tokenPanItemReader(null))
                .processor(tokenPanBinItemProcessor(binStoreService, null))
                .writer(classifierTokenCompositeItemWriter())
                .faultTolerant()
                .skipLimit(skipLimit)
                .noSkip(FileNotFoundException.class)
                .noSkip(SkipLimitExceededException.class)
                .skip(Exception.class)
                .noRetry(DateTimeParseException.class)
                .noRollback(DateTimeParseException.class)
                .noRetry(ConstraintViolationException.class)
                .noRollback(ConstraintViolationException.class)
                .listener(tokensItemReaderListener(transactionWriterService,executionDate))
                .listener(tokensItemProcessListener(transactionWriterService,executionDate))
                .listener(tokensItemWriteListener(transactionWriterService,executionDate))
                .listener(tokenPanStepListener(transactionWriterService, executionDate))
                .stream(tokenPanItemWriter(null, null))
                .stream(tokenPanFilteredItemWriter(null,null))
                .taskExecutor(batchConfig.readerTaskExecutor())
                .build();
    }

    public Step simpleWorkerStep(TokenPanStoreService tokenPANStoreService,
                                 TransactionWriterService transactionWriterService,
                                 String executionDate) throws Exception {
            return stepBuilderFactory.get("token-pan-worker-step")
                    .<InboundTokenPan, InboundTokenPan>chunk(chunkSize)
                    .reader(tokenPanItemReader(null))
                    .processor(tokenPanItemProcessor(tokenPANStoreService, null))
                    .writer(classifierTokenCompositeItemWriter())
                    .faultTolerant()
                    .skipLimit(skipLimit)
                    .noSkip(FileNotFoundException.class)
                    .noSkip(SkipLimitExceededException.class)
                    .skip(Exception.class)
                    .noRetry(DateTimeParseException.class)
                    .noRollback(DateTimeParseException.class)
                    .noRetry(ConstraintViolationException.class)
                    .noRollback(ConstraintViolationException.class)
                    .listener(tokensItemReaderListener(transactionWriterService,executionDate))
                    .listener(tokensItemProcessListener(transactionWriterService,executionDate))
                    .listener(tokensItemWriteListener(transactionWriterService,executionDate))
                    .listener(tokenPanStepListener(transactionWriterService, executionDate))
                    .stream(tokenPanItemWriter(null, null))
                    .stream(tokenPanFilteredItemWriter(null,null))
                    .taskExecutor(batchConfig.readerTaskExecutor())
                    .build();
    }

    @Bean
    public TokenPanReaderStepListener tokenPanStepListener(
            TransactionWriterService transactionWriterService, String executionDate) {
        TokenPanReaderStepListener tokenPanReaderStepListener = new TokenPanReaderStepListener();
        tokenPanReaderStepListener.setTransactionWriterService(transactionWriterService);
        tokenPanReaderStepListener.setErrorTransactionsLogsPath(tokenPanLogsPath);
        tokenPanReaderStepListener.setExecutionDate(executionDate);
        return tokenPanReaderStepListener;
    }

    @Bean
    public TokenItemReaderListener tokensItemReaderListener(
            TransactionWriterService transactionWriterService, String executionDate) {
        TokenItemReaderListener tokenPanItemReaderListener = new TokenItemReaderListener();
        tokenPanItemReaderListener.setExecutionDate(executionDate);
        tokenPanItemReaderListener.setTransactionWriterService(transactionWriterService);
        tokenPanItemReaderListener.setErrorTransactionsLogsPath(tokenPanLogsPath);
        tokenPanItemReaderListener.setEnableAfterReadLogging(enableAfterReadLogging);
        tokenPanItemReaderListener.setLoggingFrequency(loggingFrequency);
        tokenPanItemReaderListener.setTransactionWriterService(transactionWriterService);
        tokenPanItemReaderListener.setEnableOnErrorFileLogging(enableOnReadErrorFileLogging);
        tokenPanItemReaderListener.setEnableOnErrorLogging(enableOnReadErrorLogging);
        return tokenPanItemReaderListener;
    }

    @Bean
    public TokenItemWriterListener tokensItemWriteListener(
            TransactionWriterService transactionWriterService, String executionDate) {
        TokenItemWriterListener tokenItemWriterListener = new TokenItemWriterListener();
        tokenItemWriterListener.setExecutionDate(executionDate);
        tokenItemWriterListener.setTransactionWriterService(transactionWriterService);
        tokenItemWriterListener.setErrorTransactionsLogsPath(tokenPanLogsPath);
        tokenItemWriterListener.setEnableAfterWriteLogging(enableAfterWriteLogging);
        tokenItemWriterListener.setLoggingFrequency(loggingFrequency);
        tokenItemWriterListener.setTransactionWriterService(transactionWriterService);
        tokenItemWriterListener.setEnableOnErrorFileLogging(enableOnWriteErrorFileLogging);
        tokenItemWriterListener.setEnableOnErrorLogging(enableOnWriteErrorLogging);
        return tokenItemWriterListener;
    }

    @Bean
    public TokenItemProcessListener tokensItemProcessListener(
            TransactionWriterService transactionWriterService,String executionDate) {
        TokenItemProcessListener tokenItemProcessListener = new TokenItemProcessListener();
        tokenItemProcessListener.setExecutionDate(executionDate);
        tokenItemProcessListener.setErrorTransactionsLogsPath(tokenPanLogsPath);
        tokenItemProcessListener.setEnableAfterProcessLogging(enableAfterProcessLogging);
        tokenItemProcessListener.setLoggingFrequency(loggingFrequency);
        tokenItemProcessListener.setEnableOnErrorFileLogging(enableOnProcessErrorFileLogging);
        tokenItemProcessListener.setEnableOnErrorLogging(enableOnProcessErrorLogging);
        tokenItemProcessListener.setEnableAfterProcessFileLogging(enableAfterProcessFileLogging);
        tokenItemProcessListener.setTransactionWriterService(transactionWriterService);
        return tokenItemProcessListener;
    }

    /**
     *
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws Exception
     */
    @Bean
    @JobScope
    public Partitioner tokenSenderPartitioner() throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] emptyList = {};
        partitioner.setResources(tokenSenderEnabled ? resolver.getResources(localdirectory)  : emptyList);
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     *
     * @return master step to be used as the formal main step in the reading phase of the job,
     * partitioned for scalability on multiple file reading
     * @throws Exception
     */
    @Bean
    public Step tokenSenderMasterStep(SftpConnectorService sftpConnectorService) throws Exception {
        return stepBuilderFactory.get("token-sender-master-step").partitioner(
                tokenSenderWorkerStep(sftpConnectorService))
                .partitioner("partition", tokenSenderPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     *
     * @return step instance based on the tasklet to be used for file archival at the end of the reading process
     */
    @SneakyThrows
    @Bean
    public Step tokenSenderWorkerStep(SftpConnectorService sftpConnectorService) {

        return stepBuilderFactory.get("token-send-step").tasklet(
                tokenSenderTasklet(null, sftpConnectorService)).build();
    }

    @SneakyThrows
    @Bean
    @StepScope
    public TransactionSenderTasklet tokenSenderTasklet(
            @Value("#{stepExecutionContext['fileName']}") String file,
            SftpConnectorService sftpConnectorService
    ) {
        TransactionSenderTasklet transactionSenderTasklet = new TransactionSenderTasklet();
        transactionSenderTasklet.setResource(new UrlResource(file));
        transactionSenderTasklet.setSftpConnectorService(sftpConnectorService);
        transactionSenderTasklet.setTaskletEnabled(tokenSenderEnabled);
        return transactionSenderTasklet;
    }

    @Bean
    public ClassifierCompositeItemWriter<InboundTokenPan> classifierTokenCompositeItemWriter() throws Exception {
        ClassifierCompositeItemWriter<InboundTokenPan> compositeItemWriter =
                new ClassifierCompositeItemWriter<>();
        compositeItemWriter.setClassifier(
                new InboundTokenPanClassifier(
                        tokenPanItemWriter(null, null),
                        tokenPanFilteredItemWriter(null,null)));
        return compositeItemWriter;
    }

}
