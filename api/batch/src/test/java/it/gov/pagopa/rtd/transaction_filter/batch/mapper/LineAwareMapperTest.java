package it.gov.pagopa.rtd.transaction_filter.batch.mapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;


public class LineAwareMapperTest {

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
        lineAwareMapper.setFilename("test.csv");
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setDelimiter(";");
        delimitedLineTokenizer.setNames(
                "codice_acquirer", "tipo_operazione", "tipo_circuito", "PAN", "timestamp", "id_trx_acquirer",
                "id_trx_issuer", "correlation_id", "importo", "currency", "acquirerID", "merchantID", "terminal_id",
                "bank_identification_number", "MCC", "vat", "pos_type", "par");
        lineAwareMapper.setTokenizer(delimitedLineTokenizer);
        lineAwareMapper.setFieldSetMapper(new InboundTransactionFieldSetMapper("MM/dd/yyyy HH:mm:ss"));
    }

    @Test
    public void testMapper() {
        String line = "13131;00;00;pan1;03/20/2020 10:50:33;1111111111;5555;;1111;896;22222;0000;1;000002;5422;12345678901;00;";
        InboundTransaction expected = InboundTransaction.builder()
                .acquirerCode("13131")
                .operationType("00")
                .circuitType("00")
                .pan("pan1")
                .trxDate("03/20/2020 10:50:33")
                .idTrxAcquirer("1111111111")
                .idTrxIssuer("5555")
                .correlationId("")
                .amount(1111L)
                .amountCurrency("896")
                .acquirerId("22222")
                .merchantId("0000")
                .terminalId("1")
                .bin("000002")
                .mcc("5422")
                .filename("test.csv")
                .lineNumber(1)
                .vat("12345678901")
                .posType("00")
                .build();
        InboundTransaction inboundTransaction = lineAwareMapper.mapLine(line, 1);
        Assert.assertEquals(expected, inboundTransaction);
        Assert.assertEquals((Integer) 1, inboundTransaction.getLineNumber());
        Assert.assertEquals("test.csv", inboundTransaction.getFilename());
    }

    @Test
    public void testMapperThrowExceptionWhenSuperiorNumberOfColumns() {
        String line = "12;13131;00;00;pan1;03/20/2020 10:50:33;1111111111;5555;;1111;896;22222;0000;1;000002;5422;12345678901;00;par1";
        thrown.expect(FlatFileParseException.class);
        thrown.expectMessage("Parsing error at line: 1");
        lineAwareMapper.mapLine(line, 1);
    }

    @Test
    public void testMapperThrowExceptionWhenInferiorNumberOfColumns() {
        String line = "00;00;pan1;03/20/2020 10:50:33;1111111111;5555;;1111;896;22222;0000;1;000002;5422;12345678901;00";
        thrown.expect(FlatFileParseException.class);
        thrown.expectMessage("Parsing error at line: 1");
        lineAwareMapper.mapLine(line, 1);
    }

    @Test
    public void testMapperThrowExceptionWhenDatetimeIsInvalid() {
        String line = "13131;00;00;pan1;03/20/2020T10:50:33;1111111111;5555;;1111;896;22222;0000;1;000002;5422;12345678901;00;";
        thrown.expect(FlatFileParseException.class);
        lineAwareMapper.mapLine(line, 1);
    }
}