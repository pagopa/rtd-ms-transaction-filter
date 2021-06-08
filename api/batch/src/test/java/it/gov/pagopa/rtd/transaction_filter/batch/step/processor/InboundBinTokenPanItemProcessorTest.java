package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import it.gov.pagopa.rtd.transaction_filter.service.BinStoreService;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class InboundBinTokenPanItemProcessorTest {

    public InboundBinTokenPanItemProcessorTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    private BinStoreService binStoreServiceMock;

    @Before
    public void setUp() {
        Mockito.reset(binStoreServiceMock);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void process_OK_returnValid() {
        try {
            BDDMockito.doReturn(true).when(binStoreServiceMock).hasBin(Mockito.eq("00000"));
            InboundBinTokenPanItemProcessor inboundBinTokenPanItemProcessor =
                    new InboundBinTokenPanItemProcessor(binStoreServiceMock, false, true);
            List<String> exemptedCircuitType = new ArrayList<>();
            exemptedCircuitType.add("02");
            inboundBinTokenPanItemProcessor.setExemptedCircuitType(exemptedCircuitType);
            InboundTokenPan inboundTokenPan =
                    inboundBinTokenPanItemProcessor.process(getInboundTokenPan());
            Assert.assertNotNull(inboundTokenPan);
            Assert.assertEquals(getInboundTokenPan(), inboundTokenPan);
            BDDMockito.verify(binStoreServiceMock).hasBin("00000");
        } catch (Exception e) {
            Assert.fail();
            e.printStackTrace();
        }
    }


    @Test
    public void process_OK_returnNotValid() {
        try {
            BDDMockito.doReturn(true).when(binStoreServiceMock).hasBin(Mockito.eq("00001"));
            InboundBinTokenPanItemProcessor inboundBinTokenPanItemProcessor =
                    new InboundBinTokenPanItemProcessor(binStoreServiceMock, false, true);
            List<String> exemptedCircuitType = new ArrayList<>();
            exemptedCircuitType.add("02");
            inboundBinTokenPanItemProcessor.setExemptedCircuitType(exemptedCircuitType);
            InboundTokenPan inboundTokenPan =
                    inboundBinTokenPanItemProcessor.process(getInboundTokenPan());
            Assert.assertNotNull(inboundTokenPan);
            Assert.assertNotEquals(getInboundTokenPan().getValid(), inboundTokenPan.getValid());
            BDDMockito.verify(binStoreServiceMock).hasBin("00000");
        } catch (Exception e) {
            Assert.fail();
            e.printStackTrace();
        }
    }

    @Test
    public void process_OK_returnNotValid_LastSection() {
        try {
            BDDMockito.doReturn(true).when(binStoreServiceMock).hasBin(Mockito.eq("00001"));
            InboundBinTokenPanItemProcessor inboundBinTokenPanItemProcessor =
                    new InboundBinTokenPanItemProcessor(binStoreServiceMock, true, true);
            List<String> exemptedCircuitType = new ArrayList<>();
            exemptedCircuitType.add("02");
            inboundBinTokenPanItemProcessor.setExemptedCircuitType(exemptedCircuitType);
            InboundTokenPan inboundTokenPan =
                    inboundBinTokenPanItemProcessor.process(getInboundTokenPan());
            Assert.assertNull(inboundTokenPan);
            BDDMockito.verify(binStoreServiceMock).hasBin("00000");
        } catch (Exception e) {
            Assert.fail();
            e.printStackTrace();
        }
    }

    @Test
    public void process_OK_returnValid_ExemptedCard() {
        try {
            BDDMockito.doReturn(true).when(binStoreServiceMock).hasBin(Mockito.eq("00001"));
            InboundBinTokenPanItemProcessor inboundBinTokenPanItemProcessor =
                    new InboundBinTokenPanItemProcessor(binStoreServiceMock,
                            false, true);
            List<String> exemptedCircuitType = new ArrayList<>();
            exemptedCircuitType.add("01");
            inboundBinTokenPanItemProcessor.setExemptedCircuitType(exemptedCircuitType);
            InboundTokenPan inboundTokenPan =
                    inboundBinTokenPanItemProcessor.process(getInboundTokenPan());
            Assert.assertNotNull(inboundTokenPan);
            Assert.assertEquals(getInboundTokenPan(), inboundTokenPan);
            BDDMockito.verifyZeroInteractions(binStoreServiceMock);
        } catch (Exception e) {
            Assert.fail();
            e.printStackTrace();
        }
    }

    protected InboundTokenPan getInboundTokenPan() {
        return InboundTokenPan.builder()
                .tokenPan("00000")
                .circuitType("01")
                .valid(true)
                .par("0000")
                .build();
    }

}