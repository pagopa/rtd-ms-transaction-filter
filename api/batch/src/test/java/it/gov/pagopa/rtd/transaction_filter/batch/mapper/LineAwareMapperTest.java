package it.gov.pagopa.rtd.transaction_filter.batch.mapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;


public class LineAwareMapperTest {

    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXXX";
    private static final String FILENAME = "test.csv";

    public LineAwareMapperTest() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    private LineAwareMapper<InboundTransaction> lineAwareMapper;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        lineAwareMapper = new LineAwareMapper<>();
        lineAwareMapper.setFilename(FILENAME);
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setDelimiter(";");
        delimitedLineTokenizer.setNames(
                "codice_sender", "tipo_operazione", "tipo_circuito", "PAN", "timestamp", "id_trx_acquirer",
                "id_trx_issuer", "correlation_id", "importo", "currency", "acquirerID", "merchantID", "terminal_id",
                "bank_identification_number", "MCC", "fiscal_code", "vat", "pos_type", "par");
        lineAwareMapper.setTokenizer(delimitedLineTokenizer);
        lineAwareMapper.setFieldSetMapper(new InboundTransactionFieldSetMapper(DATETIME_FORMAT));
    }

    @Test
    public void testMapperValidLineWithOnlyMandatoryFields() {
        String line = "13131;00;00;pan1;2011-12-03T10:15:30.000+00:00;1111111111;5555;;1111;;22222;0000;1;000002;5422;fc123543;;00;";
        InboundTransaction expected = InboundTransaction.builder()
                .senderCode("13131")
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
                .filename(FILENAME)
                .lineNumber(1)
                .build();
        InboundTransaction inboundTransaction = lineAwareMapper.mapLine(line, 1);
        Assert.assertEquals(expected, inboundTransaction);
        Assert.assertEquals((Integer) 1, inboundTransaction.getLineNumber());
        Assert.assertEquals(FILENAME, inboundTransaction.getFilename());
    }

    @Test
    public void testMapperThrowExceptionWhenSuperiorNumberOfColumns() {
        String line = "12;13131;00;00;pan1;2011-12-03T10:15:30.000+00:00;1111111111;5555;;1111;896;22222;0000;1;000002;5422;fc123543;12345678901;00;par1";
        thrown.expect(FlatFileParseException.class);
        thrown.expectMessage("Parsing error at line: 1");
        lineAwareMapper.mapLine(line, 1);
    }

    @Test
    public void testMapperThrowExceptionWhenInferiorNumberOfColumns() {
        String line = "00;00;pan1;2011-12-03T10:15:30.000+00:00;1111111111;5555;;1111;896;22222;0000;1;000002;5422;fc123543;12345678901;00";
        thrown.expect(FlatFileParseException.class);
        thrown.expectMessage("Parsing error at line: 1");
        lineAwareMapper.mapLine(line, 1);
    }

    @Test
    public void testMapperThrowExceptionWhenDatetimeIsInvalid() {
        String line = "13131;00;00;pan1;03/20/2020T10:50:33;1111111111;5555;;1111;896;22222;0000;1;000002;5422;fc123543;12345678901;00;";
        thrown.expect(FlatFileParseException.class);
        lineAwareMapper.mapLine(line, 1);
    }
}