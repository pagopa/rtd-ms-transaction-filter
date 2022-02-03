package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class PGPFlatFileItemWriterTest {

    public PGPFlatFileItemWriterTest() {
        MockitoAnnotations.initMocks(this);
    }

    private PathMatchingResourcePatternResolver resolver;
    private String publicKey;

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @SneakyThrows
    @Before
    public void setUp() {
        resolver = new PathMatchingResourcePatternResolver();

        String publicKeyPath = "file:/" + this.getClass().getResource("/test-encrypt").getFile() + "/publicKey.asc";
        Resource publicKeyResource = resolver.getResource(publicKeyPath);
        FileInputStream publicKeyFilePathIS = new FileInputStream(publicKeyResource.getFile());
        publicKey = IOUtils.toString(publicKeyFilePathIS, "UTF-8");
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    public BeanWrapperFieldExtractor transactionWriterFieldExtractor() {
        BeanWrapperFieldExtractor<InboundTransaction> extractor = new BeanWrapperFieldExtractor<>();
        extractor.setNames(new String[]{
                "acquirerCode", "operationType", "circuitType", "pan", "trxDate", "idTrxAcquirer",
                "idTrxIssuer", "correlationId", "amount", "amountCurrency", "acquirerId", "merchantId",
                "terminalId", "bin", "mcc", "fiscalCode", "vat", "posType", "par"});
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
    public void testWriterWithEncryptionDisabledAndEmptyContent() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(publicKey, false);
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "FilteredRecords_test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        flatFileItemWriter.write(Collections.emptyList());
        flatFileItemWriter.close();

        List<String> fileContentLines = Files.readAllLines(resource.getFile().toPath());
        Assert.assertEquals(0, fileContentLines.size());
    }

    @SneakyThrows
    @Test
    public void testWriterWithEncryptionDisabledAndSingleTransaction() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(publicKey, false);
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "FilteredRecords_test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        flatFileItemWriter.write(Collections.singletonList(getInboundTransaction()));
        flatFileItemWriter.close();

        List<String> fileContentLines = Files.readAllLines(resource.getFile().toPath());
        Assert.assertEquals(1, fileContentLines.size());
        Assert.assertEquals("13131;00;00;pan1;2011-12-03T10:15:30.000+00:00;1111111111;5555;;1111;;22222;0000;1;000002;5422;fc123543;12345678901;00;", fileContentLines.get(0));
    }

    @SneakyThrows
    @Test
    public void testWriterWithEncryptionDisabledAndMultipleTransactions() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(publicKey, false);
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "FilteredRecords_test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        flatFileItemWriter.write(Collections.nCopies(5, getInboundTransaction()));
        flatFileItemWriter.close();

        List<String> fileContentLines = Files.readAllLines(resource.getFile().toPath());
        Assert.assertEquals(5, fileContentLines.size());
        for (String line : fileContentLines) {
            Assert.assertEquals("13131;00;00;pan1;2011-12-03T10:15:30.000+00:00;1111111111;5555;;1111;;22222;0000;1;000002;5422;fc123543;12345678901;00;", line);
        }
    }

    @SneakyThrows
    @Test
    public void testWriterWithEncryptionEnabledAndSingleTransaction() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(publicKey, true);
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "FilteredRecords_test-trx.csv");
        UrlResource encryptedFile = new UrlResource(tempFolder.getRoot().toURI() + "FilteredRecords_test-trx.csv.pgp");
        UrlResource decryptedFile = new UrlResource(tempFolder.getRoot().toURI() + "/decrypted/FilteredRecords_test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        flatFileItemWriter.write(Collections.singletonList(getInboundTransaction()));
        flatFileItemWriter.close();

        FileInputStream fileToProcessIS = new FileInputStream(encryptedFile.getFile());

        String secretKeyPath = "file:/" + this.getClass().getResource("/test-encrypt").getFile() + "/secretKey.asc";
        Resource secretKeyResource = resolver.getResource(secretKeyPath);
        FileInputStream secretFilePathIS = new FileInputStream(secretKeyResource.getFile());
        try {
            byte[] decryptFileData = EncryptUtil.decryptFile(fileToProcessIS, secretFilePathIS, "test".toCharArray());
            FileUtils.writeByteArrayToFile(decryptedFile.getFile(), decryptFileData);
            List<String> fileContentLines = Files.readAllLines(decryptedFile.getFile().toPath());
            Assert.assertEquals(1, fileContentLines.size());
            Assert.assertEquals("13131;00;00;pan1;2011-12-03T10:15:30.000+00:00;1111111111;5555;;1111;;22222;0000;1;000002;5422;fc123543;12345678901;00;", fileContentLines.get(0));
        } catch (Exception e) {
            Assert.fail();
        } finally {
            fileToProcessIS.close();
            secretFilePathIS.close();
        }
    }

    @SneakyThrows
    @Test
    public void testWriterWithEncryptionEnabledFailsWhenPublicKeyIsEmptyString() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter("", true);
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "FilteredRecords_test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        flatFileItemWriter.write(Collections.singletonList(getInboundTransaction()));
        exceptionRule.expect(IllegalArgumentException.class);
        flatFileItemWriter.close();
    }

    @SneakyThrows
    @Test
    public void testWriterWithEncryptionEnabledFailsWhenPublicKeyIsMalformed() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter("sfjkl24rjkldjfklsf", true);
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "FilteredRecords_test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        flatFileItemWriter.write(Collections.singletonList(getInboundTransaction()));
        exceptionRule.expect(IOException.class);
        flatFileItemWriter.close();
    }

    @SneakyThrows
    @Test
    public void testWriterWithEncryptionEnabledFailsWhenItemsIsNull() {
        PGPFlatFileItemWriter flatFileItemWriter = new PGPFlatFileItemWriter(publicKey, false);
        UrlResource resource = new UrlResource(tempFolder.getRoot().toURI() + "FilteredRecords_test-trx.csv");
        flatFileItemWriter.setResource(resource);
        flatFileItemWriter.setLineAggregator(transactionWriterAggregator());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemWriter.open(executionContext);
        flatFileItemWriter.update(executionContext);
        exceptionRule.expect(NullPointerException.class);
        flatFileItemWriter.write(null);
        flatFileItemWriter.close();
    }

    protected InboundTransaction getInboundTransaction() {
        return InboundTransaction.builder()
                .acquirerCode("13131")
                .operationType("00")
                .circuitType("00")
                .pan("pan1")
                .trxDate("2011-12-03T10:15:30.000+00:00")
                .idTrxAcquirer("1111111111")
                .idTrxIssuer("5555")
                .amount(1111L)
                .acquirerId("22222")
                .merchantId("0000")
                .terminalId("1")
                .bin("000002")
                .mcc("5422")
                .fiscalCode("fc123543")
                .vat("12345678901")
                .posType("00")
                .filename("filename")
                .lineNumber(1)
                .build();
    }

}