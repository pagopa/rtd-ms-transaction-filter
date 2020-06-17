package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Collections;

public class PGPFlatFileItemWriterTest {

    public PGPFlatFileItemWriterTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    public BeanWrapperFieldExtractor transactionWriterFieldExtractor() {
        BeanWrapperFieldExtractor<InboundTransaction> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(new String[] {
                "acquirerCode", "operationType", "circuitType", "pan", "trxDate", "idTrxAcquirer",
                "idTrxIssuer", "correlationId", "amount", "amountCurrency", "acquirerId", "merchantId", "mcc"});
        return extractor;
    }

    public LineAggregator transactionWriterAggregator() {
        DelimitedLineAggregator<InboundTransaction> delimitedLineTokenizer = new DelimitedLineAggregator<>();
        delimitedLineTokenizer.setDelimiter(";");
        delimitedLineTokenizer.setFieldExtractor(transactionWriterFieldExtractor());
        return delimitedLineTokenizer;
    }

    @SneakyThrows
    @Test
    public void testWriter_OK_EmptyList() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(
                "file:/"+this.getClass().getResource("/test-encrypt").getFile() +
                        "/secretKey.asc", false
        );
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        flatFileItemWriter.write(Collections.emptyList());
        flatFileItemWriter.close();
        Assert.assertEquals(0, Files.readAllLines(resource.getFile().toPath()).size());
    }

    @SneakyThrows
    @Test
    public void testWriter_OK_MonoList() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(
                "file:/"+this.getClass().getResource("/test-encrypt").getFile() +
                        "/secretKey.asc", false
        );
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        flatFileItemWriter.write(Collections.singletonList(getInboundTransaction()));
        flatFileItemWriter.close();
        Assert.assertEquals(1, Files.readAllLines(resource.getFile().toPath()).size());
    }

    @SneakyThrows
    @Test
    public void testWriter_OK_MultiList() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(
                "file:/"+this.getClass().getResource("/test-encrypt").getFile() +
                        "/secretKey.asc", false
        );
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        flatFileItemWriter.write(Collections.nCopies(5, getInboundTransaction()));
        flatFileItemWriter.close();
        Assert.assertEquals(5, Files.readAllLines(resource.getFile().toPath()).size());
    }

    @SneakyThrows
    @Test
    public void testWriter_OK_Encrypt() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(
                "file:/"+this.getClass().getResource("/test-encrypt").getFile() +
                        "/publicKey.asc", true
        );
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "test-trx.csv");
        UrlResource encryptedFile = new UrlResource(tempFolder.getRoot().toURI() + "test-trx.csv.pgp");
        UrlResource decryptedFile = new UrlResource(tempFolder.getRoot().toURI() + "/decrypted/test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        flatFileItemWriter.write(Collections.singletonList(getInboundTransaction()));
        flatFileItemWriter.close();

        FileInputStream fileToProcessIS = new FileInputStream(encryptedFile.getFile());

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource secretKeyResource = resolver.getResource("file:/"+
                this.getClass().getResource("/test-encrypt").getFile() +
                "/secretKey.asc");
        FileInputStream secretFilePathIS = new FileInputStream(secretKeyResource.getFile());
        try {
            byte[] decryptFileData = EncryptUtil.decryptFile(
                    fileToProcessIS,
                    secretFilePathIS,
                    "test".toCharArray()
            );
            FileUtils.writeByteArrayToFile(
                    decryptedFile.getFile(), decryptFileData);
            Assert.assertEquals(1,Files.readAllLines(decryptedFile.getFile().toPath()).size());
        } catch (Exception e) {
            Assert.fail();
        } finally {
            fileToProcessIS.close();
            secretFilePathIS.close();
        }

    }

    @SneakyThrows
    @Test
    public void testWriter_KO_Encrypt() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(
                "file:/"+this.getClass().getResource("/test-encrypt").getFile() +
                        "/wrongKey.asc", true
        );
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        flatFileItemWriter.write(Collections.singletonList(getInboundTransaction()));
        exceptionRule.expect(Exception.class);
        flatFileItemWriter.close();

    }

    @SneakyThrows
    @Test
    public void testWriter_KO_NullList() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(
                "file:/"+this.getClass().getResource("/test-encrypt").getFile() +
                        "/wrongKey.asc", false
        );
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        exceptionRule.expect(Exception.class);
        flatFileItemWriter.write(null);
        flatFileItemWriter.close();

    }

    protected InboundTransaction getInboundTransaction() {
        return InboundTransaction.builder()
                .idTrxAcquirer("1")
                .acquirerCode("001")
                .trxDate(OffsetDateTime.parse("2020-04-09T16:22:45.304Z"))
                .amount(BigDecimal.valueOf(1313.13))
                .operationType("00")
                .pan("pan")
                .merchantId("0")
                .circuitType("00")
                .mcc("813")
                .idTrxIssuer("0")
                .amountCurrency("833")
                .correlationId("1")
                .acquirerId("0")
                .build();
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}