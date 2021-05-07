package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import it.gov.pagopa.rtd.transaction_filter.service.TokenPanStoreService;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class InboundTokenPanItemProcessorTest {

    public InboundTokenPanItemProcessorTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    private TokenPanStoreService tokenPanStoreService;

    @Before
    public void setUp() {
        Mockito.reset(tokenPanStoreService);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Test
    public void process_OK_returnValid() {
        try {
            BDDMockito.doReturn(true).when(tokenPanStoreService).hasTokenPAN(Mockito.eq("00001"));
            InboundTokenPanItemProcessor inboundBinTokenPanItemProcessor =
                    new InboundTokenPanItemProcessor(tokenPanStoreService, true, false);
            InboundTokenPan inboundTokenPan =
                    inboundBinTokenPanItemProcessor.process(getInboundTokenPan());
            Assert.assertNotNull(inboundTokenPan);
            Assert.assertEquals(getInboundTokenPan(), inboundTokenPan);
            BDDMockito.verify(tokenPanStoreService).hasTokenPAN("00000");
        } catch (Exception e) {
            Assert.fail();
            e.printStackTrace();
        }
    }


    @Test
    public void process_OK_returnNotValid() {
        try {
            BDDMockito.doReturn(true).when(tokenPanStoreService).hasTokenPAN(Mockito.eq("0001"));
            InboundTokenPanItemProcessor inboundBinTokenPanItemProcessor =
                    new InboundTokenPanItemProcessor(tokenPanStoreService, false, false);
            InboundTokenPan inboundTokenPan =
                    inboundBinTokenPanItemProcessor.process(getInboundTokenPan());
            Assert.assertNotNull(inboundTokenPan);
            Assert.assertNotEquals(getInboundTokenPan().getValid(), inboundTokenPan.getValid());
            BDDMockito.verify(tokenPanStoreService).hasTokenPAN("00000");
        } catch (Exception e) {
            Assert.fail();
            e.printStackTrace();
        }
    }

    @Test
    public void process_OK_returnNotValid_LastSection() {
        try {
            BDDMockito.doReturn(true).when(tokenPanStoreService).hasTokenPAN(Mockito.eq("00000"));
            InboundTokenPanItemProcessor inboundBinTokenPanItemProcessor =
                    new InboundTokenPanItemProcessor(tokenPanStoreService, true, false);
            InboundTokenPan inboundTokenPan =
                    inboundBinTokenPanItemProcessor.process(getInboundTokenPan());
            Assert.assertNull(inboundTokenPan);
            BDDMockito.verify(tokenPanStoreService).hasTokenPAN("00000");
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