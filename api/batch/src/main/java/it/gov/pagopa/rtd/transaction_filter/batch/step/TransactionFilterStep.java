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
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.TransactionSenderTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.PGPFlatFileItemWriter;
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

import java.io.FileNotFoundException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@DependsOn({"partitionerTaskExecutor","readerTaskExecutor"})
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
    @Value("${batchConfiguration.TransactionFilterBatch.transactionSender.enabled}")
    private Boolean transactionSenderEnabled;
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

    private final BatchConfig batchConfig;
    private final StepBuilderFactory stepBuilderFactory;
    private ExecutorService writerExecutor;

    /**
     *
     * @return instance of the LineTokenizer to be used in the itemReader configured for the job
     */
    @Bean
    public LineTokenizer transactionLineTokenizer() {
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setDelimiter(";");
        delimitedLineTokenizer.setNames(
                "codice_acquirer", "tipo_operazione", "tipo_circuito", "PAN", "timestamp", "id_trx_acquirer",
                "id_trx_issuer", "correlation_id", "importo", "currency", "acquirerID", "merchantID", "terminal_id",
                "bank_identification_number", "MCC");
        return delimitedLineTokenizer;
    }

    /**
     *
     * @return instance of the FieldSetMapper to be used in the itemReader configured for the job
     */
    @Bean
    public FieldSetMapper<InboundTransaction> transactionFieldSetMapper() {
        return new InboundTransactionFieldSetMapper(timestampPattern);
    }

    /**
     *
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
     *
     * @param file
     *          Late-Binding parameter to be used as the resource for the reader instance
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
     *
     * @return instance of the LineTokenizer to be used in the itemReader configured for the job
     */
    @Bean
    public BeanWrapperFieldExtractor<InboundTransaction> transactionWriterFieldExtractor() {
        BeanWrapperFieldExtractor<InboundTransaction> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(new String[] {
                "acquirerCode", "operationType", "circuitType", "pan", "trxDate", "idTrxAcquirer",
                "idTrxIssuer", "correlationId", "amount", "amountCurrency", "acquirerId", "merchantId",
                "terminalId", "bin", "mcc"});
        return extractor;
    }

    /**
     *
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
     *
     * @param file
     *          Late-Binding parameter to be used as the resource for the reader instance
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
    public FlatFileItemWriter<InboundTransaction> transactionFilteredItemWriter(
            @Value("#{stepExecutionContext['fileName']}") String file) {
        FlatFileItemWriter<InboundTransaction> flatFileItemWriter = new FlatFileItemWriter<>();
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        file = file.replaceAll("\\\\", "/");
        String[] filename = file.split("/");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        flatFileItemWriter.setResource(
                resolver.getResource(transactionLogsPath.concat("/"
                        .concat("FilteredRecords_"+filename[filename.length-1]))));
        return flatFileItemWriter;
    }


    /**
     *
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
     *
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws Exception
     */
    @Bean
    @JobScope
    public Partitioner transactionFilterPartitioner() throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        partitioner.setResources(resolver.getResources(transactionDirectoryPath));
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
    public Step transactionFilterMasterStep(HpanStoreService hpanStoreService,
                                            TransactionWriterService transactionWriterService)
            throws Exception {
        return stepBuilderFactory.get("transaction-filter-master-step").partitioner(
                transactionFilterWorkerStep(hpanStoreService,transactionWriterService))
                .partitioner("partition", transactionFilterPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     *
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     * @throws Exception
     */
    @Bean
    public Step transactionFilterWorkerStep(HpanStoreService hpanStoreService, TransactionWriterService transactionWriterService)
            throws Exception {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);
        return simpleWorkerStep(hpanStoreService, transactionWriterService, executionDate);
    }


    public Step simpleWorkerStep(HpanStoreService hpanStoreService, TransactionWriterService transactionWriterService, String executionDate) throws Exception {
            return stepBuilderFactory.get("transaction-filter-worker-step")
                    .<InboundTransaction, InboundTransaction>chunk(chunkSize)
                    .reader(transactionItemReader(null))
                    .processor(transactionItemProcessor(hpanStoreService))
                    .writer(transactionItemWriter(null))
                    .faultTolerant()
                    .skipLimit(skipLimit)
                    .noSkip(FileNotFoundException.class)
                    .skip(Exception.class)
                    .listener(transactionItemReaderListener(executionDate))
                    .listener(transactionsItemProcessListener(transactionWriterService,executionDate))
                    .listener(transactionsItemWriteListener(executionDate))
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
        return transactionReaderStepListener;
    }

    @Bean
    public TransactionItemReaderListener transactionItemReaderListener(String executionDate) {
        TransactionItemReaderListener transactionItemReaderListener = new TransactionItemReaderListener();
        transactionItemReaderListener.setExecutionDate(executionDate);
        transactionItemReaderListener.setErrorTransactionsLogsPath(transactionLogsPath);
        transactionItemReaderListener.setEnableAfterReadLogging(enableAfterReadLogging);
        transactionItemReaderListener.setLoggingFrequency(loggingFrequency);
        transactionItemReaderListener.setEnableOnErrorFileLogging(enableOnReadErrorFileLogging);
        transactionItemReaderListener.setEnableOnErrorLogging(enableOnReadErrorLogging);
        return transactionItemReaderListener;
    }

    @Bean
    public TransactionItemWriterListener transactionsItemWriteListener(String executionDate) {
        TransactionItemWriterListener transactionsItemWriteListener = new TransactionItemWriterListener();
        transactionsItemWriteListener.setExecutionDate(executionDate);
        transactionsItemWriteListener.setErrorTransactionsLogsPath(transactionLogsPath);
        transactionsItemWriteListener.setEnableAfterWriteLogging(enableAfterWriteLogging);
        transactionsItemWriteListener.setLoggingFrequency(loggingFrequency);
        transactionsItemWriteListener.setEnableOnErrorFileLogging(enableOnWriteErrorFileLogging);
        transactionsItemWriteListener.setEnableOnErrorLogging(enableOnWriteErrorLogging);
        return transactionsItemWriteListener;
    }

    @Bean
    public TransactionItemProcessListener transactionsItemProcessListener(
            TransactionWriterService transactionWriterService,String executionDate) {
        TransactionItemProcessListener transactionItemProcessListener = new TransactionItemProcessListener();
        transactionItemProcessListener.setExecutionDate(executionDate);
        transactionItemProcessListener.setErrorTransactionsLogsPath(transactionLogsPath);
        transactionItemProcessListener.setEnableAfterProcessLogging(enableAfterProcessLogging);
        transactionItemProcessListener.setLoggingFrequency(loggingFrequency);
        transactionItemProcessListener.setEnableOnErrorFileLogging(enableOnProcessErrorFileLogging);
        transactionItemProcessListener.setEnableOnErrorLogging(enableOnProcessErrorLogging);
        transactionItemProcessListener.setEnableAfterProcessFileLogging(enableAfterProcessFileLogging);
        transactionItemProcessListener.setTransactionWriterService(transactionWriterService);
        return transactionItemProcessListener;
    }

    /**
     *
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws Exception
     */
    @Bean
    @JobScope
    public Partitioner transactionSenderPartitioner() throws Exception {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] emptyList = {};
        partitioner.setResources(transactionSenderEnabled ? resolver.getResources(localdirectory)  : emptyList);
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
    public Step transactionSenderMasterStep(SftpConnectorService sftpConnectorService) throws Exception {
        return stepBuilderFactory.get("transaction-sender-master-step").partitioner(
                transactionSenderWorkerStep(sftpConnectorService))
                .partitioner("partition", transactionSenderPartitioner())
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     *
     * @return step instance based on the tasklet to be used for file archival at the end of the reading process
     */
    @SneakyThrows
    @Bean
    public Step transactionSenderWorkerStep(SftpConnectorService sftpConnectorService) {

        return stepBuilderFactory.get("transaction-filter-send-step").tasklet(
                transactionSenderTasklet(null, sftpConnectorService)).build();
    }

    @SneakyThrows
    @Bean
    @StepScope
    public TransactionSenderTasklet transactionSenderTasklet(
            @Value("#{stepExecutionContext['fileName']}") String file,
            SftpConnectorService sftpConnectorService
    ) {
        TransactionSenderTasklet transactionSenderTasklet = new TransactionSenderTasklet();
        transactionSenderTasklet.setResource(new UrlResource(file));
        transactionSenderTasklet.setSftpConnectorService(sftpConnectorService);
        transactionSenderTasklet.setTaskletEnabled(transactionSenderEnabled);
        return transactionSenderTasklet;
    }

    /**
     *
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
