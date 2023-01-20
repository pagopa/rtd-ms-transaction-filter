package it.gov.pagopa.rtd.transaction_filter.batch.step;

import it.gov.pagopa.rtd.transaction_filter.batch.config.BatchConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.mapper.InboundTransactionFieldSetMapper;
import it.gov.pagopa.rtd.transaction_filter.batch.mapper.LineAwareMapper;
import it.gov.pagopa.rtd.transaction_filter.batch.model.AdeTransactionsAggregate;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.TransactionItemProcessListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.TransactionItemReaderListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.TransactionItemWriterListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.listener.TransactionReaderStepListener;
import it.gov.pagopa.rtd.transaction_filter.batch.step.processor.InboundTransactionItemProcessor;
import it.gov.pagopa.rtd.transaction_filter.batch.step.processor.TransactionAggregationReaderProcessor;
import it.gov.pagopa.rtd.transaction_filter.batch.step.processor.TransactionAggregationWriterProcessor;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.CustomIteratorItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.FileReportItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.reader.TransactionFlatFileItemReader;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.PGPEncrypterTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.TransactionChecksumTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet.TransactionSenderRestTasklet;
import it.gov.pagopa.rtd.transaction_filter.batch.step.writer.ChecksumHeaderWriter;
import it.gov.pagopa.rtd.transaction_filter.connector.FileReportRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient.SasScope;
import it.gov.pagopa.rtd.transaction_filter.connector.model.FileMetadata;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationKey;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.ConstraintViolationException;
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
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

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
    @Value("${batchConfiguration.TransactionFilterBatch.pendingArchivePath}")
    private String pendingDirectoryPath;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.linesToSkip}")
    private Integer linesToSkip;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.timestampPattern}")
    private String timestampPattern;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.applyHashing}")
    private Boolean applyTrxHashing;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.applyEncrypt}")
    private Boolean applyEncrypt;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionFilter.inputFileChecksumEnabled}")
    private boolean inputFileChecksumEnabled;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionSenderAde.enabled}")
    private Boolean transactionSenderAdeEnabled;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionSenderRtd.enabled}")
    private Boolean transactionSenderRtdEnabled;
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
    @Value("${batchConfiguration.TransactionFilterBatch.transactionSenderPending.enabled}")
    private Boolean transactionSenderPendingEnabled;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionWriterAde.splitThreshold}")
    private int adeSplitThreshold;
    @Value("${batchConfiguration.TransactionFilterBatch.fileReportRecovery.directoryPath}")
    private String fileReportDirectory;
    @Value("${batchConfiguration.TransactionFilterBatch.fileReportRecovery.fileNamePrefix}")
    private String fileReportPrefixName;
    @Value("${batchConfiguration.TransactionFilterBatch.transactionWriterRtd.splitThreshold}")
    private int rtdSplitThreshold;

    public static final String RTD_OUTPUT_FILE_PREFIX = "CSTAR.";
    public static final String ADE_OUTPUT_FILE_PREFIX = "ADE.";
    public static final String PAGOPA_PGP_PUBLIC_KEY_ID = "pagopa";
    private static final String LOG_PREFIX_TRN = "Rtd_";
    private static final String LOG_PREFIX_ADE = "Ade_";
    private static final String REGEX_PGP_FILES = "*.pgp";
    private static final String PARTITIONER_WORKER_STEP_NAME = "partition";
    // [service].[ABI].[filetype].[date].[time].[nnn].csv
    private static final String TRX_FILENAME_PATTERN = "^CSTAR\\.\\w{5}\\.TRNLOG\\.\\d{8}\\.\\d{6}\\.\\d{3}\\.csv$";
    private static final String[] ADE_CSV_FIELDS = new String[]{
        "senderCode", "operationType", "transmissionDate", "accountingDate", "numTrx", "totalAmount",
        "currency", "acquirerId", "merchantId", "terminalId", "fiscalCode", "vat", "posType"  };
    private static final String[] CSTAR_CSV_FIELDS = new String[]{
        "senderCode", "operationType", "circuitType", "pan", "trxDate", "idTrxAcquirer",
        "idTrxIssuer", "correlationId", "amount", "amountCurrency", "acquirerId", "merchantId",
        "terminalId", "bin", "mcc", "fiscalCode", "vat", "posType", "par"};
    private static final String[] REPORT_CSV_FIELDS = new String[]{
        "name", "status", "size", "transmissionDate"};
    private static final String CSV_DELIMITER = ";";
    private static final String DATE_FORMAT_FOR_FILENAME = "yyyyMMddHHmmssSSS";

    private final BatchConfig batchConfig;
    private final StepBuilderFactory stepBuilderFactory;
    private ExecutorService writerExecutor;

    private PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    /**
     * @return instance of the LineTokenizer to be used in the itemReader configured for the job
     */
    @Bean
    public LineTokenizer transactionLineTokenizer() {
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setDelimiter(CSV_DELIMITER);
        delimitedLineTokenizer.setNames(
                "codice_sender", "tipo_operazione", "tipo_circuito", "PAN", "timestamp", "id_trx_acquirer",
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

    public ItemReader<AggregationKey> mapItemReader(StoreService storeService) {
        return new CustomIteratorItemReader<>(storeService.getAggregateKeySet());
    }

    /**
     * Composes CSV lines from filtered transactions' models.
     *
     * @return an instance of a DelimitedLineAggregator suited for filtered aggregations data model
     */
    @Bean
    public LineAggregator<InboundTransaction> transactionWriterAggregator() {
        BeanWrapperFieldExtractor<InboundTransaction> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(CSTAR_CSV_FIELDS);
        DelimitedLineAggregator<InboundTransaction> delimitedLineAggregator = new DelimitedLineAggregator<>();
        delimitedLineAggregator.setDelimiter(CSV_DELIMITER);
        delimitedLineAggregator.setFieldExtractor(extractor);
        return delimitedLineAggregator;
    }

    /**
     * Composes CSV lines from AdE aggregates' models.
     *
     * @return an instance of a DelimitedLineAggregator suited for AdE aggregations data model
     */
    @Bean
    public LineAggregator<AdeTransactionsAggregate> adeTransactionsAggregateLineAggregator() {
        BeanWrapperFieldExtractor<AdeTransactionsAggregate> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(ADE_CSV_FIELDS);
        DelimitedLineAggregator<AdeTransactionsAggregate> delimitedLineAggregator = new DelimitedLineAggregator<>();
        delimitedLineAggregator.setDelimiter(CSV_DELIMITER);
        delimitedLineAggregator.setFieldExtractor(extractor);
        return delimitedLineAggregator;
    }

    /**
     * Composes CSV lines from file report model.
     * @return a line aggregator
     */
    @Bean
    public LineAggregator<FileMetadata> fileReportLineAggregator() {
        BeanWrapperFieldExtractor<FileMetadata> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(REPORT_CSV_FIELDS);
        DelimitedLineAggregator<FileMetadata> delimitedLineAggregator = new DelimitedLineAggregator<>();
        delimitedLineAggregator.setDelimiter(CSV_DELIMITER);
        delimitedLineAggregator.setFieldExtractor(extractor);
        return delimitedLineAggregator;
    }

    /**
     * Builds a MultiResourceItemWriter for filtered transactions.
     * The usage of the decorator SynchronizedItemStreamWriterBuilder is needed because the
     * MultiResourceItemWriter is not thread safe.
     *
     * @param filename Late-Binding parameter to be used as the resource for the reader instance
     * @param storeService data structures shared between different steps
     * @return instance of an itemWriter to be used in the transactionFilterWorkerStep
     */
    @Bean
    @StepScope
    public ItemWriter<InboundTransaction> transactionMultiResourceItemWriter(
            @Value("#{stepExecutionContext['fileName']}") String filename, StoreService storeService) {
        return new SynchronizedItemStreamWriterBuilder<InboundTransaction>()
            .delegate(new MultiResourceItemWriterBuilder<InboundTransaction>()
                .name("transaction-multi-resource-writer")
                .itemCountLimitPerResource(rtdSplitThreshold)
                .resource(resolver.getResource(outputDirectoryPath))
                .resourceSuffixCreator(index -> "/" + getOutputFileNameChunkedWithPrefix(filename, index,
                    RTD_OUTPUT_FILE_PREFIX))
                .delegate(createItemWriter(storeService, transactionWriterAggregator()))
                .build())
            .build();
    }

    /**
     * @return master step to be used as the formal main step in the file encryption,
     * partitioned for scalability on multiple files
     */
    @Bean
    public Step encryptTransactionChunksMasterStep(StoreService storeService) {
        return stepBuilderFactory.get("encrypt-transaction-chunks-master-step")
            .partitioner(encryptTransactionChunksWorkerStep(storeService))
            .partitioner(PARTITIONER_WORKER_STEP_NAME, outputRtdFilesPartitioner(storeService))
            .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    @Bean
    public Step encryptTransactionChunksWorkerStep(StoreService storeService) {
        return stepBuilderFactory
            .get("encrypt-transaction-chunks-worker-step")
            .tasklet(getPGPEncrypterTasklet(null, storeService)).build();
    }

    /**
     * MultiResourcePartitioner that matches the rtd output files generated in the current run
     * @param storeService data structures shared between different steps
     * @return a partitioner
     */
    @SneakyThrows
    @Bean
    @JobScope
    public Partitioner outputRtdFilesPartitioner(StoreService storeService) {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        // do not match every file in output directory but only the ones generated from the input file
        String outputFileRegex = getOutputFilesRegex(storeService.getTargetInputFile(), RTD_OUTPUT_FILE_PREFIX);
        String pathMatcher = outputDirectoryPath + File.separator + outputFileRegex;
        partitioner.setResources(resolver.getResources(pathMatcher));
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     * Builds an MultiResourceItemWriter for aggregated transactions with file splitting feature based on threshold.
     * Implements encryption of the output file via PGP.
     * The usage of the decorator SynchronizedItemStreamWriterBuilder is needed because the
     * MultiResourceItemWriter is not thread safe.
     *
     * @param filename Late-Binding parameter to be used as the resource for the reader instance
     * @param storeService data structures shared between different steps
     * @return instance of an itemWriter to be used in the transactionAggregationWriterWorkerStep
     */
    @Bean
    @StepScope
    public ItemWriter<AdeTransactionsAggregate> transactionAggregateMultiResourceWriter(
        @Value("#{stepExecutionContext['fileName']}") String filename, StoreService storeService) {
        return new SynchronizedItemStreamWriterBuilder<AdeTransactionsAggregate>()
            .delegate(new MultiResourceItemWriterBuilder<AdeTransactionsAggregate>()
                .name("aggregate-multi-resource-writer")
                .itemCountLimitPerResource(adeSplitThreshold)
                .resource(resolver.getResource(outputDirectoryPath))
                .resourceSuffixCreator(index -> "/" + getOutputFileNameChunkedWithPrefix(filename, index,
                    ADE_OUTPUT_FILE_PREFIX))
                .delegate(createItemWriter(storeService, adeTransactionsAggregateLineAggregator()))
                .build())
            .build();
    }

    /**
     * Produce an output filename from an input file path, a chunk number and a prefix.
     * Supports the chunking based on index.
     * @param filePath path of input file
     * @param chunkIndex index of the chunk
     * @param prefix output file prefix (e.g. ADE or CSTAR)
     * @return output filename based on chunk index
     */
    protected String getOutputFileNameChunkedWithPrefix(String filePath, int chunkIndex, String prefix) {
        String filePathTmp = filePath.replace("\\", "/");
        String[] pathSplitted = filePathTmp.split("/");
        String fileNameWithoutPrefixAndExtension = pathSplitted[pathSplitted.length - 1].substring(6,
            pathSplitted[pathSplitted.length - 1].lastIndexOf("."));
        String fileExtension = pathSplitted[pathSplitted.length - 1].substring(
            pathSplitted[pathSplitted.length - 1].lastIndexOf("."));
        return prefix.concat(fileNameWithoutPrefixAndExtension)
            .concat(String.format(".%02d", chunkIndex)).concat(fileExtension);
    }

    /**
     * Builds an ItemWriter without resource to be used in a MultiResourceItemWriter. The resource
     * will be assigned by the MultiResourceItemWriter during the step runtime.
     * @param storeService data structures shared between different steps
     * @return an item writer
     */
    protected <T> FlatFileItemWriter<T> createItemWriter(StoreService storeService, LineAggregator<T> lineAggregator) {
        FlatFileItemWriter<T> itemWriter = new FlatFileItemWriter<>();
        itemWriter.setLineAggregator(lineAggregator);
        if (inputFileChecksumEnabled) {
            ChecksumHeaderWriter checksumHeaderWriter = new ChecksumHeaderWriter(storeService.getTargetInputFileHash());
            itemWriter.setHeaderCallback(checksumHeaderWriter);
        }
        return itemWriter;
    }

    /**
     * @return master step to be used as the formal main step in the file encryption,
     * partitioned for scalability on multiple files
     */
    @Bean
    public Step encryptAggregateChunksMasterStep(StoreService storeService) {
        return stepBuilderFactory.get("encrypt-aggregate-chunks-master-step")
            .partitioner(encryptAggregateChunksWorkerStep(storeService))
            .partitioner(PARTITIONER_WORKER_STEP_NAME, outputAdeFilesPartitioner(storeService))
            .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * MultiResourcePartitioner that matches the ade output files generated in the current run
     * @param storeService data structures shared between different steps
     * @return a partitioner
     */
    @SneakyThrows
    @Bean
    @JobScope
    public Partitioner outputAdeFilesPartitioner(StoreService storeService) {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        // do not match every file in output directory but only the ones generated from the input file
        String outputFileRegex = getOutputFilesRegex(storeService.getTargetInputFile(), ADE_OUTPUT_FILE_PREFIX);
        String pathMatcher = outputDirectoryPath + File.separator + outputFileRegex;
        partitioner.setResources(resolver.getResources(pathMatcher));
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     * Turns filename into regex by removing chunk token with *
     * e.g. ADE.11111.TNRLOG.YYYYMMDD.HHMMSS.001.00.csv into ADE.11111.TNRLOG.YYYYMMDD.HHMMSS.001.*.csv
     *
     * @param inputFile input filename to be converted
     * @param prefix prefix needed to turn input filename into output one
     * @return a string containing the output files regex
     */
    private String getOutputFilesRegex(String inputFile, String prefix) {
        return getOutputFileNameChunkedWithPrefix(inputFile, 0, prefix)
            .replace(".00.", ".*.");
    }

    /**
     * Worker step that contains the tasklet step (must have as proxy since the tasklet is step scoped)
     * @param storeService data structure shared
     * @return worker Step
     */
    @Bean
    public Step encryptAggregateChunksWorkerStep(StoreService storeService) {
        return stepBuilderFactory
            .get("encrypt-aggregate-chunks-worker-step")
            .tasklet(getPGPEncrypterTasklet(null, storeService)).build();
    }

    /**
     * Tasklet that execute the file encryption of the file set in step scope (partitioner does the job)
     * @param file path of file to encrypt
     * @param storeService data structure shared
     * @return a tasklet
     */
    @SneakyThrows
    @Bean
    @StepScope
    public PGPEncrypterTasklet getPGPEncrypterTasklet(
        @Value("#{stepExecutionContext['fileName']}") String file, StoreService storeService
    ) {
        PGPEncrypterTasklet pgpEncrypterTasklet = new PGPEncrypterTasklet();
        pgpEncrypterTasklet.setPublicKey(storeService.getKey(PAGOPA_PGP_PUBLIC_KEY_ID));
        pgpEncrypterTasklet.setFileToEncrypt(new UrlResource(file));
        pgpEncrypterTasklet.setTaskletEnabled(applyEncrypt);
        return pgpEncrypterTasklet;
    }

    /**
     * Step that retrieve and write on file a file report
     * @param restClient file report rest client
     * @return a step
     */
    @Bean
    public Step fileReportRecoveryStep(FileReportRestClient restClient) {
        return stepBuilderFactory.get("file-report-recovery-step")
            .<FileMetadata, FileMetadata>chunk(chunkSize)
            .reader(fileReportReader(restClient))
            .writer(fileReportWriter())
            .faultTolerant()
            .build();
    }

    /**
     * ItemReader that retrieve a file report JSON from a rest client and converts it to FileMetadata
     * @param restClient file report rest client
     * @return a itemReader
     */
    @Bean
    public ItemReader<FileMetadata> fileReportReader(FileReportRestClient restClient) {
        return new FileReportItemReader(restClient);
    }

    /**
     * ItemWriter that save on file the file report. It implements a headerCallback with the field names
     * and a line aggregator to convert the pojo into a CSV file with ";" as delimiter.
     * @return a itemWriter
     */
    @SneakyThrows
    @Bean
    public ItemWriter<FileMetadata> fileReportWriter() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATE_FORMAT_FOR_FILENAME);
        String currentDate = OffsetDateTime.now().format(fmt);

        Resource outputResource = new FileSystemResource(resolver.getResource(
                getPathToResolve(fileReportDirectory)).getFile().getAbsolutePath()
            .concat("/")
            .concat(fileReportPrefixName)
            .concat("-")
            .concat(currentDate)
            .concat(".csv"));

        return new FlatFileItemWriterBuilder<FileMetadata>()
            .name("file-report-item-writer")
            .resource(outputResource)
            .headerCallback(writer -> writer.write(String.join(CSV_DELIMITER, REPORT_CSV_FIELDS)))
            .lineAggregator(fileReportLineAggregator())
            .build();
    }

    /**
     * Convert a path adding the prefix "file:" if it does not contain "classpath:" already. For test purpose.
     * @param directory
     * @return
     */
    private String getPathToResolve(String directory) {
        return directory.startsWith("classpath:") ? directory
            : "file:".concat(directory);
    }

    /**
     * Builds a dummy ItemWriter to use during transaction aggregation.
     * Since we're reading from a chunk-oriented ItemReader and aggregating data in-memory
     * in the ItemProcessor we declare a dummy (i.e. do nothing) ItemWriter to postpone the
     * writing of the computed aggregations in a next dedicated step.
     */
    public class NoOpItemWriter implements ItemWriter<InboundTransaction> {
        @Override
        public void write(List<? extends InboundTransaction> list) throws Exception {
            // no-op
        }
    }

    /**
     * @return instance of the itemProcessor to be used in the first step of the configured job
     */
    @Bean
    @StepScope
    public InboundTransactionItemProcessor transactionItemProcessor(
            StoreService storeService) {
        return new InboundTransactionItemProcessor(
                storeService,
                this.applyTrxHashing);
    }

    /**
     * Builds an ItemProcessor to use during transaction aggregation.
     *
     * @return instance of an itemProcessor to be used in the transactionAggregationReaderWorkerStep
     */
    @Bean
    @StepScope
    public TransactionAggregationReaderProcessor transactionAggregationProcessor(
        StoreService storeService) {
        return new TransactionAggregationReaderProcessor(
            storeService);
    }

    /**
     * Builds an ItemProcessor to use during aggregation write to file.
     *
     * @return instance of an itemProcessor to be used in the transactionAggregationWriterWorkerStep
     */
    @Bean
    @StepScope
    public TransactionAggregationWriterProcessor transactionAggregationWriterProcessor(
        StoreService storeService) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String transmissionDate = OffsetDateTime.now().format(fmt);
        return new TransactionAggregationWriterProcessor(storeService, transmissionDate);
    }

    /**
     * @return instance of a partitioner to be used for processing multiple files from a single directory
     * @throws IOException
     */
    @Bean
    @JobScope
    public Partitioner transactionFilterPartitioner(StoreService storeService) throws IOException {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        Resource[] resources = resolver.getResources(transactionDirectoryPath + "/*.csv");
        resources = filterValidFilenames(resources);
        resources = filterResourcesByFilename(resources, storeService.getTargetInputFile());
        partitioner.setResources(resources);
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     * @return master step to be used as the formal main step in the reading phase of the job,
     * partitioned for scalability on multiple file reading
     * @throws IOException
     */
    @Bean
    public Step transactionFilterMasterStep(StoreService storeService, TransactionWriterService transactionWriterService) throws IOException {
        return stepBuilderFactory.get("transaction-filter-master-step")
                .partitioner(transactionFilterWorkerStep(storeService, transactionWriterService))
                .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionFilterPartitioner(storeService))
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * @return worker step, defined as a standard reader/processor/writer process,
     * using chunk processing for scalability
     */
    @Bean
    public Step transactionFilterWorkerStep(StoreService storeService, TransactionWriterService transactionWriterService) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATE_FORMAT_FOR_FILENAME);
        String executionDate = OffsetDateTime.now().format(fmt);
        return stepBuilderFactory.get("transaction-filter-worker-step")
                .<InboundTransaction, InboundTransaction>chunk(chunkSize)
                .reader(transactionItemReader(null))
                .processor(transactionItemProcessor(storeService))
                .writer(transactionMultiResourceItemWriter(null, storeService))
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

    /**
     * Master step for the in-memory aggregation of transaction files.
     *
     * @param storeService data structures shared between different steps
     * @param transactionWriterService service exposing handlers for local log files
     * @return the aggregation reader batch master step
     * @throws IOException
     */
    @Bean
    public Step transactionAggregationReaderMasterStep(StoreService storeService, TransactionWriterService transactionWriterService) throws IOException {
        return stepBuilderFactory.get("transaction-aggregation-reader-master-step")
            .partitioner(transactionAggregationReaderWorkerStep(storeService, transactionWriterService))
            .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionFilterPartitioner(storeService))
            .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * Worker step for the in-memory aggregation of transaction files.
     *
     * @param storeService data structures shared between different steps
     * @param transactionWriterService service exposing handlers for local log files
     * @return the aggregation reader batch worker step
     */
    @Bean
    public Step transactionAggregationReaderWorkerStep(StoreService storeService, TransactionWriterService transactionWriterService) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(DATE_FORMAT_FOR_FILENAME);
        String executionDate = OffsetDateTime.now().format(fmt);
        return stepBuilderFactory.get("transaction-aggregation-reader-worker-step")
            .<InboundTransaction, InboundTransaction>chunk(chunkSize)
            .reader(transactionItemReader(null))
            .processor(transactionAggregationProcessor(storeService))
            .writer(new NoOpItemWriter())
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
            .listener(transactionAdeItemProcessListener(transactionWriterService, executionDate))
            .listener(transactionAdeStepListener(transactionWriterService, executionDate))
            .taskExecutor(batchConfig.readerTaskExecutor())
            .build();
    }

    /**
     * Master step for the writing of aggregation output files.
     *
     * @param storeService data structures shared between different steps
     * @return the aggregation writer batch master step
     * @throws IOException
     */
    @Bean
    public Step transactionAggregationWriterMasterStep(StoreService storeService) throws IOException {
        return stepBuilderFactory.get("transaction-aggregation-writer-master-step")
            .partitioner(transactionAggregationWriterWorkerStep(storeService))
            .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionFilterPartitioner(storeService))
            .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * Worker step for the writing of aggregation output files.
     *
     * @param storeService data structures shared between different steps
     * @return the aggregation writer batch worker step
     */
    @Bean
    public Step transactionAggregationWriterWorkerStep(StoreService storeService) {
        return stepBuilderFactory.get("transaction-aggregation-writer-worker-step")
            .<AggregationKey, AdeTransactionsAggregate>chunk(chunkSize)
            .reader(mapItemReader(storeService))
            .processor(transactionAggregationWriterProcessor(storeService))
            .writer(transactionAggregateMultiResourceWriter(null, storeService))
            .faultTolerant()
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

    @Bean
    public TransactionItemProcessListener transactionAdeItemProcessListener(
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
        transactionItemProcessListener.setPrefix(LOG_PREFIX_ADE);
        return transactionItemProcessListener;
    }

    /**
     * Partitioning strategy for the upload of RTD transaction files.
     *
     * @return a partitioner instance
     * @throws IOException
     */
    @Bean
    @JobScope
    public Partitioner transactionSenderRtdPartitioner(StoreService storeService) throws IOException {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        String fileNameWithoutExtension = storeService.getTargetInputFile().replace(".csv", "");
        String pathMatcher = outputDirectoryPath + File.separator + fileNameWithoutExtension + REGEX_PGP_FILES;
        partitioner.setResources(resolver.getResources(pathMatcher));
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     * Partitioning strategy for the upload of AdE transaction files.
     *
     * @return a partitioner instance
     * @throws IOException
     */
    @Bean
    @JobScope
    public Partitioner transactionSenderAdePartitioner(StoreService storeService) throws IOException {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        String fileNameWithoutExtension = storeService.getTargetInputFile().replace(".csv", "");
        String outputFilePrefix = fileNameWithoutExtension.replace("CSTAR", "ADE");
        String pathMatcher = outputDirectoryPath + File.separator + outputFilePrefix + REGEX_PGP_FILES;
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
    public Step transactionSenderAdeMasterStep(HpanConnectorService hpanConnectorService,
        StoreService storeService) throws IOException {
        return stepBuilderFactory.get("transaction-sender-ade-master-step")
            .partitioner(transactionSenderAdeWorkerStep(hpanConnectorService))
            .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionSenderAdePartitioner(storeService))
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
     * Master step for the upload of RTD transaction files.
     *
     * @param hpanConnectorService
     * @return the RTD batch master step
     * @throws IOException
     */
    @Bean
    public Step transactionSenderRtdMasterStep(HpanConnectorService hpanConnectorService,
        StoreService storeService) throws IOException {
        return stepBuilderFactory.get("transaction-sender-rtd-master-step")
                .partitioner(transactionSenderRtdWorkerStep(hpanConnectorService))
                .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionSenderRtdPartitioner(storeService))
                .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * Worker step for the upload of RTD transaction files.
     *
     * @param hpanConnectorService
     * @return the RTD batch worker step
     */
    @SneakyThrows
    @Bean
    public Step transactionSenderRtdWorkerStep(HpanConnectorService hpanConnectorService) {
        return stepBuilderFactory.get("transaction-sender-rtd-worker-step").tasklet(
                transactionSenderRtdTasklet(null, hpanConnectorService)).build();
    }

    /**
     * Tasklet responsible for the upload of RTD transaction files via REST endpoints.
     *
     * @param file the file to upload remotely via REST
     * @param hpanConnectorService
     * @return an instance configured for the upload of a specified file
     */
    @SneakyThrows
    @Bean
    @StepScope
    public TransactionSenderRestTasklet transactionSenderRtdTasklet(
            @Value("#{stepExecutionContext['fileName']}") String file,
            HpanConnectorService hpanConnectorService
    ) {
        TransactionSenderRestTasklet transactionSenderRestTasklet = new TransactionSenderRestTasklet();
        transactionSenderRestTasklet.setHpanConnectorService(hpanConnectorService);
        transactionSenderRestTasklet.setResource(new UrlResource(file));
        transactionSenderRestTasklet.setTaskletEnabled(transactionSenderRtdEnabled);
        transactionSenderRestTasklet.setScope(HpanRestClient.SasScope.RTD);
        return transactionSenderRestTasklet;
    }

    /**
     * Partitioning strategy for the upload of output files in pending directory.
     *
     * @return a partitioner instance
     * @throws IOException
     */
    @Bean
    @JobScope
    public Partitioner transactionSenderPendingPartitioner() throws IOException {
        MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
        String pathMatcher = pendingDirectoryPath + File.separator + REGEX_PGP_FILES;
        partitioner.setResources(resolver.getResources(pathMatcher));
        partitioner.partition(partitionerSize);
        return partitioner;
    }

    /**
     * Master step for the upload of output files in pending directory.
     *
     * @param hpanConnectorService service to connect to rest client
     * @return the AdE batch master step
     * @throws IOException
     */
    @Bean
    public Step transactionSenderPendingMasterStep(HpanConnectorService hpanConnectorService) throws IOException {
        return stepBuilderFactory.get("transaction-sender-pending-master-step")
            .partitioner(transactionSenderPendingWorkerStep(hpanConnectorService))
            .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionSenderPendingPartitioner())
            .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * Worker step for the upload of output files in pending directory.
     *
     * @param hpanConnectorService service to connect to rest client
     * @return the AdE batch worker step
     */
    @SneakyThrows
    @Bean
    public Step transactionSenderPendingWorkerStep(HpanConnectorService hpanConnectorService) {
        return stepBuilderFactory.get("transaction-sender-pending-worker-step").tasklet(
            transactionSenderPendingTasklet(null, hpanConnectorService)).build();
    }

    /**
     * Tasklet responsible for the upload of transaction files via REST endpoints.
     *
     * @param file the file to upload remotely via REST
     * @param hpanConnectorService service to connect to rest client
     * @return an instance configured for the upload of a specified file
     */
    @SneakyThrows
    @Bean
    @StepScope
    public TransactionSenderRestTasklet transactionSenderPendingTasklet(
        @Value("#{stepExecutionContext['fileName']}") String file,
        HpanConnectorService hpanConnectorService) {
        TransactionSenderRestTasklet transactionSenderRestTasklet = new TransactionSenderRestTasklet();
        transactionSenderRestTasklet.setHpanConnectorService(hpanConnectorService);
        transactionSenderRestTasklet.setResource(new UrlResource(file));
        transactionSenderRestTasklet.setTaskletEnabled(transactionSenderPendingEnabled);
        transactionSenderRestTasklet.setScope(getSasScopeByFileName(file));
        return transactionSenderRestTasklet;
    }

    private SasScope getSasScopeByFileName(String file) {
        return file.contains("ADE") ? SasScope.ADE : SasScope.RTD;
    }

    /**
     * Master step for the hashing of input transaction files.
     *
     * @param storeService
     * @return the hashing batch master step
     * @throws IOException
     */
    @Bean
    public Step transactionChecksumMasterStep(StoreService storeService) throws IOException {
        return stepBuilderFactory.get("transaction-checksum-master-step")
            .partitioner(transactionChecksumWorkerStep(storeService))
            .partitioner(PARTITIONER_WORKER_STEP_NAME, transactionFilterPartitioner(storeService))
            .taskExecutor(batchConfig.partitionerTaskExecutor()).build();
    }

    /**
     * Worker step for the hashing of input transaction files.
     *
     * @param storeService
     * @return the hashing batch worker step
     */
    @Bean
    public Step transactionChecksumWorkerStep(StoreService storeService)
        throws MalformedURLException {
        return stepBuilderFactory.get("transaction-checksum-worker-step").tasklet(
            transactionChecksumTasklet(null, storeService)).build();
    }

    /**
     * Tasklet responsible for computation of the hash of each input transaction file.
     *
     * @param file the file to be hashed
     * @param storeService
     * @return an instance configured for the hashing of a specified file
     */
    @Bean
    @StepScope
    public TransactionChecksumTasklet transactionChecksumTasklet(
        @Value("#{stepExecutionContext['fileName']}") String file,
        StoreService storeService
    ) throws MalformedURLException {
        TransactionChecksumTasklet transactionChecksumTasklet = new TransactionChecksumTasklet();
        transactionChecksumTasklet.setResource(new UrlResource(file));
        transactionChecksumTasklet.setStoreService(storeService);
        transactionChecksumTasklet.setTaskletEnabled(inputFileChecksumEnabled);
        return transactionChecksumTasklet;
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

    /**
     * Filter a list of resources retaining only those matching naming convention
     *
     * @param resources a list of resources to be filtered
     * @return a filtered list of resources
     */
    public static Resource[] filterValidFilenames(Resource[] resources) {
        List<Resource> filtered = new ArrayList<>();
        Pattern pattern = Pattern.compile(TRX_FILENAME_PATTERN);
        for (Resource inputResource : resources) {
            Matcher matcher = pattern.matcher(inputResource.getFilename());
            if (matcher.find()) {
                filtered.add(inputResource);
            }
        }
        return filtered.stream().toArray(Resource[]::new);
    }

    /**
     * Filter a list of resources retaining only those matching a target filename
     *
     * @param resources a list of resources to be filtered
     * @param validFilename only resource(s) matching this filename will be retained from the list
     * @return a filtered list of resources
     */
    public static Resource[] filterResourcesByFilename(Resource[] resources, String validFilename) {
        List<Resource> filtered = new ArrayList<>();
        for (Resource inputResource : resources) {
            String resFilename = inputResource.getFilename();
            if (resFilename != null && resFilename.equals(validFilename)) {
                filtered.add(inputResource);
            }
        }
        return filtered.stream().toArray(Resource[]::new);
    }

}
