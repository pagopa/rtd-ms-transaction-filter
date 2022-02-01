package it.gov.pagopa.rtd.transaction_filter.batch.step;

import it.gov.pagopa.rtd.transaction_filter.batch.config.BatchConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.TransactionItemProcessListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.TransactionItemReaderListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.TransactionItemWriterListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.TransactionReaderStepListener;
import it.gov.pagopa.rtd.transaction_filter.batch.mapper.InboundTransactionFieldSetMapper;
import it.gov.pagopa.rtd.transaction_filter.batch.mapper.LineAwareMapper;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.batch.step.processor.InboundTransactionItemProcessor;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.TransactionFlatFileItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.TransactionSenderRestTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.TransactionSenderTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.PGPFlatFileItemWriter;
import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.SftpConnectorService;
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
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.validation.ConstraintViolationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@DependsOn({"partitionerTaskExecutor", "readerTaskExecutor"})
@RequiredArgsConstructor
@Data
@PropertySource("classpath:config/transactionFilterStep.properties")
public class TransactionFilterStep {

    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.partitionerSize}")
    private Integer partitionerSize;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.chunkSize}")
    private Integer chunkSize;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.skipLimit}")
    private Integer skipLimit;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath}")
    private String transactionDirectoryPath;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath}")
    private String outputDirectoryPath;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.publicKeyPath}")
    private String publicKeyPath;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.linesToSkip}")
    private Integer linesToSkip;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.timestampPattern}")
    private String timestampPattern;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.applyHashing}")
    private Boolean applyTrxHashing;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.applyEncrypt}")
    private Boolean applyEncrypt;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.sftp.localdirectory}")
    private String localdirectory;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionSenderFtp.enabled}")
    private boolean transactionSenderFtpEnabled;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionSenderAde.enabled}")
    private Boolean transactionSenderAdeEnabled;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionSenderCstar.enabled}")
    private Boolean transactionSenderCstarEnabled;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.transactionLogsPath}")
    private String transactionLogsPath;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterReadLogging}")
    private Boolean enableAfterReadLogging;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnReadErrorFileLogging}")
    private Boolean enableOnReadErrorFileLogging;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnReadErrorLogging}")
    private Boolean enableOnReadErrorLogging;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessLogging}")
    private Boolean enableAfterProcessLogging;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessFileLogging}")
    private Boolean enableAfterProcessFileLogging;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnProcessErrorFileLogging}")
    private Boolean enableOnProcessErrorFileLogging;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnProcessErrorLogging}")
    private Boolean enableOnProcessErrorLogging;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterWriteLogging}")
    private Boolean enableAfterWriteLogging;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnWriteErrorFileLogging}")
    private Boolean enableOnWriteErrorFileLogging;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnWriteErrorLogging}")
    private Boolean enableOnWriteErrorLogging;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.loggingFrequency}")
    private Long loggingFrequency;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.writerPoolSize}")
    private Integer writerPoolSize;

    public static final String CSTAR_OUTPUT_FILE_PREFIX = "CSTAR.";
    public static final String ADE_OUTPUT_FILE_PREFIX = "ADE.";
    private static final String LOG_PREFIX_TRN = "Trn_";
    private static final String LOG_PREFIX_ADE = "Ade_";
    private static final String PARTITIONER_WORKER_STEP_NAME = "partition";

    private final BatchConfig batchConfig;
    private final StepBuilderFactory stepBuilderFactory;
    private ExecutorService writerExecutor;

    /**
     * @return instance of the LineTokenizer to be used in the itemReader configured for the job
     */
    @Bean
    public LineTokenizer transactionLineTokenizer() {
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setDelimiter(";");
        delimitedLineTokenizer.setNames(
                "codice_acquirer", "tipo_operazione", "tipo_circuito", "PAN", "timestamp", "id_trx_acquirer",
                "id_trx_issuer", "correlation_id", "importo", "currency", "acquirerID", "merchantID", "terminal_id",
                "bank_identification_number", "MCC", "fiscal_code", "vat", "pos_type", "par");
        return delimitedLineTokenizer;
    }

    /**
     * @return instance of the FieldSetMapper to be used in the itemReader configured for the job
     */
    @Bean
    public FieldSetMapper<InboundTransaction> transactionFieldSetMapper() {
        return new InboundTransactionFieldSetMapper(timestampPattern);
    }

    /**
     * @return instance of the LineMapper to be used in the itemReader configured for the job
     */
    public LineMapper<InboundTransaction> transactionLineMapper(String fileName) {
        LineAwareMapper<InboundTransaction> lineMapper = new LineAwareMapper<>();
        lineMapper.setTokenizer(transactionLineTokenizer());
        lineMapper.setFieldSetMapper(transactionFieldSetMapper());
        lineMapper.setFilename(fileName);
        return lineMapper;
    }

    /**
     * @param file Late-Binding parameter to be used as the resource for the reader instance
     * @return instance of the itemReader to be used in the first step of the configured job
     */
    @SneakyThrows
    @Bean
    @StepScope
    public TransactionFlatFileItemReader transactionItemReader(
            @Value("#{stepExecutionContext['fileName']}") String file) {
        TransactionFlatFileItemReader flatFileItemReader = new TransactionFlatFileItemReader();
        flatFileItemReader.setResource(new UrlResource(file));
        flatFileItemReader.setLineMapper(transactionLineMapper(file));
        flatFileItemReader.setLinesToSkip(linesToSkip);
        return flatFileItemReader;
    }

    /**
     * @return instance of the LineTokenizer to be used in the itemReader configured for the job
     */
    @Bean
    public BeanWrapperFieldExtractor<InboundTransaction> transactionWriterFieldExtractor() {
        BeanWrapperFieldExtractor<InboundTransaction> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(new String[]{
                "acquirerCode", "operationType", "circuitType", "pan", "trxDate", "idTrxAcquirer",
                "idTrxIssuer", "correlationId", "amount", "amountCurrency", "acquirerId", "merchantId",
                "terminalId", "bin", "mcc", "fiscalCode", "vat", "posType", "par"});
        return extractor;
    }

    /**
     * @return instance of the LineTokenizer to be used in the itemReader configured for the job
     */
    @Bean
    public BeanWrapperFieldExtractor<InboundTransaction> transactionAdeWriterFieldExtractor() {
        BeanWrapperFieldExtractor<InboundTransaction> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(new String[]{
                "acquirerCode", "operationType", "circuitType", "trxDate", "idTrxAcquirer",
                "idTrxIssuer", "correlationId", "amount", "amountCurrency", "acquirerId", "merchantId",
                "terminalId", "bin", "mcc", "fiscalCode", "vat", "posType", "par"});
        return extractor;
    }

    /**
     * @return instance of the LineTokenizer to be used in the itemReader configured for the job
     */
    @Bean
    public LineAggregator<InboundTransaction> transactionWriterAggregator() {
        DelimitedLineAggregator<InboundTransaction> delimitedLineTokenizer = new DelimitedLineAggregator<>();
        delimitedLineTokenizer.setDelimiter(";");
        delimitedLineTokenizer.setFieldExtractor(transactionWriterFieldExtractor());
        return delimitedLineTokenizer;
    }

    /**
     * @return instance of the LineTokenizer to be used in the itemReader configured for the job
     */
    @Bean
    public LineAggregator<InboundTransaction> transactionAdeWriterAggregator() {
        DelimitedLineAggregator<InboundTransaction> delimitedLineTokenizer = new DelimitedLineAggregator<>();
        delimitedLineTokenizer.setDelimiter(";");
        delimitedLineTokenizer.setFieldExtractor(transactionAdeWriterFieldExtractor());
        return delimitedLineTokenizer;
    }

    /**
     * @param file Late-Binding parameter to be used as the resource for the reader instance
     * @return instance of the itemReader to be used in the first step of the configured job
     */
    @SneakyThrows
    @Bean
    @StepScope
    public PGPFlatFileItemWriter transactionItemWriter(
            @Value("#{stepExecutionContext['fileName']}") String file) {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(publicKeyPath, applyEncrypt);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        file = file.replaceAll("\\\\", "/");
        String[] filename = file.split("/");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        flatFileItemWriter.setResource(
                resolver.getResource(outputDirectoryPath.concat("/".concat(filename[filename.length - 1]))));
        return flatFileItemWriter;
    }

    /**
     * Create the ItemWriter to be used in the step 'transaction-filter-ade-worker-step'
     *
     * @param file name of the file being processed
     * @return an instance of ItemWriter
     */
    @SneakyThrows
    @Bean
    @StepScope
    public PGPFlatFileItemWriter transactionAdeItemWriter(
            @Value("#{stepExecutionContext['fileName']}") String file) {
        PGPFlatFileItemWriter itemWriter = new PGPFlatFileItemWriter(publicKeyPath, applyEncrypt);
        itemWriter.setLineAggregator(transactionAdeWriterAggregator());
        file = file.replaceAll("\\\\", "/");
        String[] filename = file.split("/");
        String newFilename = ADE_OUTPUT_FILE_PREFIX + filename[filename.length - 1];
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        itemWriter.setResource(resolver.getResource(outputDirectoryPath.concat("/".concat(newFilename))));
        return itemWriter;
    }

    /**
     * @return instance of the itemProcessor to be used in the first step of the configured job
     */
    @Bean
    @StepScope
    public InboundTransactionItemProcessor transactionItemProcessor(
            HpanStoreService hpanStoreService) {
        return new InboundTransactionItemProcessor(
                hpanStoreService,
                this.applyTrxHashing);
    }

    /**
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws IOException
     */
    @Bean
    @JobScope
    public Partitioner transactionFilterPartitioner() throws IOException {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(transactionDirectoryPath));
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     * Build the master step for AdE batch processing.
     *
     * @param transactionWriterService an instance of TransactionWriterService shared between steps
     * @return the AdE batch master step
     * @throws IOException
     */
    @Bean
    public Step transactionFilterAdeMasterStep(TransactionWriterService transactionWriterService) throws IOException {
        return stepBuilderFactory
                .get("transaction-filter-ade-master-step")
                .partitioner(transactionFilterAdeWorkerStep(transactionWriterService))
                .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionFilterPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * Build the worker step for AdE batch processing.
     *
     * @param transactionWriterService an instance of TransactionWriterService shared between steps
     * @return the AdE batch worker step
     */
    @Bean
    public Step transactionFilterAdeWorkerStep(TransactionWriterService transactionWriterService) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);
        return stepBuilderFactory.get("transaction-filter-ade-worker-step")
                .<InboundTransaction, InboundTransaction>chunk(chunkSize)
                .reader(transactionItemReader(null))
                .writer(transactionAdeItemWriter(null))
                .faultTolerant()
                .skipLimit(skipLimit)
                .noSkip(FileNotFoundException.class)
                .noSkip(SkipLimitExceededException.class)
                .skip(Exception.class)
                .noRetry(DateTimeParseException.class)
                .noRollback(DateTimeParseException.class)
                .noRetry(ConstraintViolationException.class)
                .noRollback(ConstraintViolationException.class)
                .listener(transactionAdeItemReaderListener(transactionWriterService, executionDate))
                .listener(transactionAdeItemWriteListener(transactionWriterService, executionDate))
                .listener(transactionAdeStepListener(transactionWriterService, executionDate))
                .taskExecutor(batchConfig.readerTaskExecutor())
                .build();
    }

    /**
     * @return master step to be used as the formal main step in the reading phase of the job,
     * partitioned for scalability on multiple file reading
     * @throws IOException
     */
    @Bean
    public Step transactionFilterMasterStep(HpanStoreService hpanStoreService, TransactionWriterService transactionWriterService) throws IOException {
        return stepBuilderFactory.get("transaction-filter-master-step")
                .partitioner(transactionFilterWorkerStep(hpanStoreService, transactionWriterService))
                .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionFilterPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     */
    @Bean
    public Step transactionFilterWorkerStep(HpanStoreService hpanStoreService, TransactionWriterService transactionWriterService) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);
        return stepBuilderFactory.get("transaction-filter-worker-step")
                .<InboundTransaction, InboundTransaction>chunk(chunkSize)
                .reader(transactionItemReader(null))
                .processor(transactionItemProcessor(hpanStoreService))
                .writer(transactionItemWriter(null))
                .faultTolerant()
                .skipLimit(skipLimit)
                .noSkip(FileNotFoundException.class)
                .noSkip(SkipLimitExceededException.class)
                .skip(Exception.class)
                .noRetry(DateTimeParseException.class)
                .noRollback(DateTimeParseException.class)
                .noRetry(ConstraintViolationException.class)
                .noRollback(ConstraintViolationException.class)
                .listener(transactionItemReaderListener(transactionWriterService, executionDate))
                .listener(transactionItemProcessListener(transactionWriterService, executionDate))
                .listener(transactionItemWriteListener(transactionWriterService, executionDate))
                .listener(transactionStepListener(transactionWriterService, executionDate))
                .taskExecutor(batchConfig.readerTaskExecutor())
                .build();
    }

    @Bean
    public TransactionReaderStepListener transactionStepListener(
            TransactionWriterService transactionWriterService, String executionDate) {
        TransactionReaderStepListener transactionReaderStepListener = new TransactionReaderStepListener();
        transactionReaderStepListener.setTransactionWriterService(transactionWriterService);
        transactionReaderStepListener.setErrorTransactionsLogsPath(transactionLogsPath);
        transactionReaderStepListener.setExecutionDate(executionDate);
        transactionReaderStepListener.setPrefix(LOG_PREFIX_TRN);
        return transactionReaderStepListener;
    }

    @Bean
    public TransactionReaderStepListener transactionAdeStepListener(
            TransactionWriterService transactionWriterService, String executionDate) {
        TransactionReaderStepListener transactionReaderStepListener = new TransactionReaderStepListener();
        transactionReaderStepListener.setTransactionWriterService(transactionWriterService);
        transactionReaderStepListener.setErrorTransactionsLogsPath(transactionLogsPath);
        transactionReaderStepListener.setExecutionDate(executionDate);
        transactionReaderStepListener.setPrefix(LOG_PREFIX_ADE);
        return transactionReaderStepListener;
    }

    @Bean
    public TransactionItemReaderListener transactionItemReaderListener(
            TransactionWriterService transactionWriterService, String executionDate) {
        TransactionItemReaderListener transactionItemReaderListener = new TransactionItemReaderListener();
        transactionItemReaderListener.setExecutionDate(executionDate);
        transactionItemReaderListener.setTransactionWriterService(transactionWriterService);
        transactionItemReaderListener.setErrorTransactionsLogsPath(transactionLogsPath);
        transactionItemReaderListener.setEnableAfterReadLogging(enableAfterReadLogging);
        transactionItemReaderListener.setLoggingFrequency(loggingFrequency);
        transactionItemReaderListener.setEnableOnErrorFileLogging(enableOnReadErrorFileLogging);
        transactionItemReaderListener.setEnableOnErrorLogging(enableOnReadErrorLogging);
        transactionItemReaderListener.setPrefix(LOG_PREFIX_TRN);
        return transactionItemReaderListener;
    }

    @Bean
    public TransactionItemReaderListener transactionAdeItemReaderListener(
            TransactionWriterService transactionWriterService, String executionDate) {
        TransactionItemReaderListener transactionItemReaderListener = new TransactionItemReaderListener();
        transactionItemReaderListener.setExecutionDate(executionDate);
        transactionItemReaderListener.setTransactionWriterService(transactionWriterService);
        transactionItemReaderListener.setErrorTransactionsLogsPath(transactionLogsPath);
        transactionItemReaderListener.setEnableAfterReadLogging(enableAfterReadLogging);
        transactionItemReaderListener.setLoggingFrequency(loggingFrequency);
        transactionItemReaderListener.setEnableOnErrorFileLogging(enableOnReadErrorFileLogging);
        transactionItemReaderListener.setEnableOnErrorLogging(enableOnReadErrorLogging);
        transactionItemReaderListener.setPrefix(LOG_PREFIX_ADE);
        return transactionItemReaderListener;
    }

    @Bean
    public TransactionItemWriterListener transactionItemWriteListener(
            TransactionWriterService transactionWriterService, String executionDate) {
        TransactionItemWriterListener transactionItemWriteListener = new TransactionItemWriterListener();
        transactionItemWriteListener.setExecutionDate(executionDate);
        transactionItemWriteListener.setTransactionWriterService(transactionWriterService);
        transactionItemWriteListener.setErrorTransactionsLogsPath(transactionLogsPath);
        transactionItemWriteListener.setEnableAfterWriteLogging(enableAfterWriteLogging);
        transactionItemWriteListener.setLoggingFrequency(loggingFrequency);
        transactionItemWriteListener.setEnableOnErrorFileLogging(enableOnWriteErrorFileLogging);
        transactionItemWriteListener.setEnableOnErrorLogging(enableOnWriteErrorLogging);
        transactionItemWriteListener.setPrefix(LOG_PREFIX_TRN);
        return transactionItemWriteListener;
    }

    @Bean
    public TransactionItemWriterListener transactionAdeItemWriteListener(
            TransactionWriterService transactionWriterService, String executionDate) {
        TransactionItemWriterListener transactionItemWriteListener = new TransactionItemWriterListener();
        transactionItemWriteListener.setExecutionDate(executionDate);
        transactionItemWriteListener.setTransactionWriterService(transactionWriterService);
        transactionItemWriteListener.setErrorTransactionsLogsPath(transactionLogsPath);
        transactionItemWriteListener.setEnableAfterWriteLogging(enableAfterWriteLogging);
        transactionItemWriteListener.setLoggingFrequency(loggingFrequency);
        transactionItemWriteListener.setEnableOnErrorFileLogging(enableOnWriteErrorFileLogging);
        transactionItemWriteListener.setEnableOnErrorLogging(enableOnWriteErrorLogging);
        transactionItemWriteListener.setPrefix(LOG_PREFIX_ADE);
        return transactionItemWriteListener;
    }

    @Bean
    public TransactionItemProcessListener transactionItemProcessListener(
            TransactionWriterService transactionWriterService, String executionDate) {
        TransactionItemProcessListener transactionItemProcessListener = new TransactionItemProcessListener();
        transactionItemProcessListener.setExecutionDate(executionDate);
        transactionItemProcessListener.setErrorTransactionsLogsPath(transactionLogsPath);
        transactionItemProcessListener.setEnableAfterProcessLogging(enableAfterProcessLogging);
        transactionItemProcessListener.setLoggingFrequency(loggingFrequency);
        transactionItemProcessListener.setEnableOnErrorFileLogging(enableOnProcessErrorFileLogging);
        transactionItemProcessListener.setEnableOnErrorLogging(enableOnProcessErrorLogging);
        transactionItemProcessListener.setEnableAfterProcessFileLogging(enableAfterProcessFileLogging);
        transactionItemProcessListener.setTransactionWriterService(transactionWriterService);
        transactionItemProcessListener.setPrefix(LOG_PREFIX_TRN);
        return transactionItemProcessListener;
    }

    /**
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws IOException
     */
    @Bean
    @JobScope
    public Partitioner transactionSenderFtpPartitioner() throws IOException {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] emptyList = {};
        partitioner.setResources(transactionSenderFtpEnabled ? resolver.getResources(localdirectory) : emptyList);
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     * @return master step to be used as the formal main step in the reading phase of the job,
     * partitioned for scalability on multiple file reading
     * @throws IOException
     */
    @Bean
    public Step transactionSenderFtpMasterStep(SftpConnectorService sftpConnectorService) throws IOException {
        return stepBuilderFactory.get("transaction-sender-ftp-master-step")
                .partitioner(transactionSenderFtpWorkerStep(sftpConnectorService))
                .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionSenderFtpPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * @return step instance based on the tasklet to be used for file archival at the end of the reading process
     */
    @SneakyThrows
    @Bean
    public Step transactionSenderFtpWorkerStep(SftpConnectorService sftpConnectorService) {

        return stepBuilderFactory.get("transaction-sender-ftp-worker-step").tasklet(
                transactionSenderFtpTasklet(null, sftpConnectorService)).build();
    }

    @SneakyThrows
    @Bean
    @StepScope
    public TransactionSenderTasklet transactionSenderFtpTasklet(
            @Value("#{stepExecutionContext['fileName']}") String file,
            SftpConnectorService sftpConnectorService
    ) {
        TransactionSenderTasklet transactionSenderTasklet = new TransactionSenderTasklet();
        transactionSenderTasklet.setResource(new UrlResource(file));
        transactionSenderTasklet.setSftpConnectorService(sftpConnectorService);
        transactionSenderTasklet.setTaskletEnabled(transactionSenderFtpEnabled);
        return transactionSenderTasklet;
    }

    /**
     * Partitioning strategy for the upload of AdE transaction files.
     *
     * @return a partitioner instance
     * @throws IOException
     */
    @Bean
    @JobScope
    public Partitioner transactionSenderAdePartitioner() throws IOException {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String pathMatcher = outputDirectoryPath + File.separator + ADE_OUTPUT_FILE_PREFIX + "*.pgp";
        partitioner.setResources(resolver.getResources(pathMatcher));
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     * Master step for the upload of AdE transaction files.
     *
     * @param hpanConnectorService
     * @return the AdE batch master step
     * @throws IOException
     */
    @Bean
    public Step transactionSenderAdeMasterStep(HpanConnectorService hpanConnectorService) throws IOException {
        return stepBuilderFactory.get("transaction-sender-ade-master-step")
                .partitioner(transactionSenderAdeWorkerStep(hpanConnectorService))
                .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionSenderAdePartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * Worker step for the upload of AdE transaction files.
     *
     * @param hpanConnectorService
     * @return the AdE batch worker step
     */
    @SneakyThrows
    @Bean
    public Step transactionSenderAdeWorkerStep(HpanConnectorService hpanConnectorService) {
        return stepBuilderFactory.get("transaction-sender-ade-worker-step").tasklet(
                transactionSenderAdeTasklet(null, hpanConnectorService)).build();
    }

    /**
     * Tasklet responsible for the upload of AdE transaction files via REST endpoints.
     *
     * @param file the file to upload remotely via REST
     * @param hpanConnectorService
     * @return an instance configured for the upload of a specified file
     */
    @SneakyThrows
    @Bean
    @StepScope
    public TransactionSenderRestTasklet transactionSenderAdeTasklet(
            @Value("#{stepExecutionContext['fileName']}") String file,
            HpanConnectorService hpanConnectorService
    ) {
        TransactionSenderRestTasklet transactionSenderRestTasklet = new TransactionSenderRestTasklet();
        transactionSenderRestTasklet.setHpanConnectorService(hpanConnectorService);
        transactionSenderRestTasklet.setResource(new UrlResource(file));
        transactionSenderRestTasklet.setTaskletEnabled(transactionSenderAdeEnabled);
        transactionSenderRestTasklet.setScope(HpanRestClient.SasScope.ADE);
        return transactionSenderRestTasklet;
    }

    /**
     * Partitioning strategy for the upload of CSTAR transaction files.
     *
     * @return a partitioner instance
     * @throws IOException
     */
    @Bean
    @JobScope
    public Partitioner transactionSenderCstarPartitioner() throws IOException {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String pathMatcher = outputDirectoryPath + File.separator + CSTAR_OUTPUT_FILE_PREFIX + "*.pgp";
        partitioner.setResources(resolver.getResources(pathMatcher));
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     * Master step for the upload of CSTAR transaction files.
     *
     * @param hpanConnectorService
     * @return the CSTAR batch master step
     * @throws IOException
     */
    @Bean
    public Step transactionSenderCstarMasterStep(HpanConnectorService hpanConnectorService) throws IOException {
        return stepBuilderFactory.get("transaction-sender-cstar-master-step")
                .partitioner(transactionSenderCstarWorkerStep(hpanConnectorService))
                .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionSenderCstarPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * Worker step for the upload of CSTAR transaction files.
     *
     * @param hpanConnectorService
     * @return the CSTAR batch worker step
     */
    @SneakyThrows
    @Bean
    public Step transactionSenderCstarWorkerStep(HpanConnectorService hpanConnectorService) {
        return stepBuilderFactory.get("transaction-sender-cstar-worker-step").tasklet(
                transactionSenderCstarTasklet(null, hpanConnectorService)).build();
    }

    /**
     * Tasklet responsible for the upload of CSTAR transaction files via REST endpoints.
     *
     * @param file the file to upload remotely via REST
     * @param hpanConnectorService
     * @return an instance configured for the upload of a specified file
     */
    @SneakyThrows
    @Bean
    @StepScope
    public TransactionSenderRestTasklet transactionSenderCstarTasklet(
            @Value("#{stepExecutionContext['fileName']}") String file,
            HpanConnectorService hpanConnectorService
    ) {
        TransactionSenderRestTasklet transactionSenderRestTasklet = new TransactionSenderRestTasklet();
        transactionSenderRestTasklet.setHpanConnectorService(hpanConnectorService);
        transactionSenderRestTasklet.setResource(new UrlResource(file));
        transactionSenderRestTasklet.setTaskletEnabled(transactionSenderCstarEnabled);
        transactionSenderRestTasklet.setScope(HpanRestClient.SasScope.CSTAR);
        return transactionSenderRestTasklet;
    }

    /**
     * @return bean configured for usage for chunk reading of a single file
     */
    @Bean
    public ExecutorService writerExecutor() {
        if (writerExecutor == null) {
            writerExecutor = Executors.newFixedThreadPool(writerPoolSize);
        }
        return writerExecutor;
    }

}
