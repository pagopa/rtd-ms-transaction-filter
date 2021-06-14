package it.gov.pagopa.rtd.transaction_filter.batch.mapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import lombok.SneakyThrows;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;

public class InboundTokenPanLineAwareMapperTest {

    public InboundTokenPanLineAwareMapperTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    private InboundTokenPanLineAwareMapper<InboundTokenPan> lineAwareMapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        lineAwareMapper = new InboundTokenPanLineAwareMapper<>();
        lineAwareMapper.setFilename("test.csv");
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
        delimitedLineTokenizer.setDelimiter(";");
        delimitedLineTokenizer.setNames(
                "token_pan", "circuit_type", "par");
        lineAwareMapper.setTokenizer(delimitedLineTokenizer);
        lineAwareMapper.setFieldSetMapper(new InboundTokenPanFieldSetMapper());
    }

    @Test
    public void testMapper() {

        try {
            InboundTokenPan inboundTransaction = lineAwareMapper.mapLine(
                    "00000;01;0000",
                    1);
            Assert.assertEquals(getInboundTransaction(), inboundTransaction);
            Assert.assertEquals((Integer) 1, inboundTransaction.getLineNumber());
            Assert.assertEquals("test.csv", inboundTransaction.getFilename());
        } catch (Exception exception) {
            exception.printStackTrace();
            Assert.fail();
        }

    }

    @SneakyThrows
    @Test
    public void testMapper_KO() {

        expectedException.expect(FlatFileParseException.class);
        lineAwareMapper.mapLine(
                "01;0000",
                1);

    }

    public InboundTokenPan getInboundTransaction() {
        return InboundTokenPan.builder()
                .tokenPan("00000")
                .circuitType("01")
                .par("0000")
                .valid(true)
                .filename("test.csv")
                .lineNumber(1)
                .build();
    }

}