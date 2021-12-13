package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

public class InboundTransactionItemProcessorTest  {

    public InboundTransactionItemProcessorTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    private HpanStoreService hpanStoreServiceMock;

    @Before
    public void setUp() {
        Mockito.reset(hpanStoreServiceMock);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void process_OK_noHashing() {
        try {
            BDDMockito.doReturn(true).when(hpanStoreServiceMock).hasHpan(Mockito.eq("pan"));
            BDDMockito.doReturn("testSalt").when(hpanStoreServiceMock).getSalt();
            InboundTransactionItemProcessor inboundTransactionItemProcessor =
                    new InboundTransactionItemProcessor(hpanStoreServiceMock, false);
            InboundTransaction inboundTransaction =
                    inboundTransactionItemProcessor.process(getInboundTransaction());
            Assert.assertNotNull(inboundTransaction);
            Assert.assertEquals(getInboundTransaction(), inboundTransaction);
            BDDMockito.verify(hpanStoreServiceMock).hasHpan(Mockito.eq("pan"));
        } catch (Exception e) {
            Assert.fail();
            e.printStackTrace();
        }
    }

    @Test
    public void process_OK_NoPan_DisabledLogging() {
        BDDMockito.doReturn(false).when(hpanStoreServiceMock).hasHpan(Mockito.eq("pan"));
        BDDMockito.doReturn("testSalt").when(hpanStoreServiceMock).getSalt();
        InboundTransactionItemProcessor inboundTransactionItemProcessor =
                new InboundTransactionItemProcessor(hpanStoreServiceMock, false);
        InboundTransaction inboundTransaction =
                inboundTransactionItemProcessor.process(getInboundTransaction());
        Assert.assertNull(inboundTransaction);
        BDDMockito.verify(hpanStoreServiceMock).hasHpan(Mockito.eq("pan"));
    }

    @Test
    public void process_OK_ApplyHashing() {
        String hashPan = DigestUtils.sha256Hex("pan" + "testSalt");
        BDDMockito.doReturn(true).when(hpanStoreServiceMock).hasHpan(Mockito.eq(hashPan));
        BDDMockito.doReturn("testSalt").when(hpanStoreServiceMock).getSalt();
        InboundTransactionItemProcessor inboundTransactionItemProcessor =
                new InboundTransactionItemProcessor(hpanStoreServiceMock, true);
        InboundTransaction inboundTransaction =
                inboundTransactionItemProcessor.process(getInboundTransaction());
        Assert.assertNotNull(inboundTransaction);
        Assert.assertEquals(getInboundTransaction(), inboundTransaction);
        BDDMockito.verify(hpanStoreServiceMock).hasHpan(Mockito.eq(hashPan));
    }

    @Test
    public void process_OK_SaveHashing() {
        BDDMockito.doReturn(true).when(hpanStoreServiceMock).hasHpan(Mockito.eq("pan"));
        BDDMockito.doReturn("testSalt").when(hpanStoreServiceMock).getSalt();
        InboundTransactionItemProcessor inboundTransactionItemProcessor =
                new InboundTransactionItemProcessor(hpanStoreServiceMock,false);
        InboundTransaction inboundTransaction =
                inboundTransactionItemProcessor.process(getInboundTransaction());
        Assert.assertNotNull(inboundTransaction);
        InboundTransaction expectedTransaction = getInboundTransaction();
        expectedTransaction.setPan(DigestUtils.sha256Hex(expectedTransaction.getPan()+"testSalt"));
        Assert.assertEquals(expectedTransaction, inboundTransaction);
        BDDMockito.verify(hpanStoreServiceMock).hasHpan(Mockito.eq("pan"));
    }


    @Test
    public void process_OK_AllHashing() {
        String hashPan = DigestUtils.sha256Hex("pan" + "testSalt");
        BDDMockito.doReturn(true).when(hpanStoreServiceMock).hasHpan(Mockito.eq(hashPan));
        BDDMockito.doReturn("testSalt").when(hpanStoreServiceMock).getSalt();
        InboundTransactionItemProcessor inboundTransactionItemProcessor =
                new InboundTransactionItemProcessor(hpanStoreServiceMock,true);
        InboundTransaction inboundTransaction =
                inboundTransactionItemProcessor.process(getInboundTransaction());
        Assert.assertNotNull(inboundTransaction);
        InboundTransaction expectedTransaction = getInboundTransaction();
        expectedTransaction.setPan(DigestUtils.sha256Hex(expectedTransaction.getPan()+"testSalt"));
        Assert.assertEquals(expectedTransaction, inboundTransaction);
        BDDMockito.verify(hpanStoreServiceMock).hasHpan(Mockito.eq(hashPan));
    }

    @Test
    public void process_OK_AllHashing_NoSalt() {
        String hashPan = DigestUtils.sha256Hex("pan");
        BDDMockito.doReturn(true).when(hpanStoreServiceMock).hasHpan(Mockito.eq(hashPan));
        BDDMockito.doReturn("").when(hpanStoreServiceMock).getSalt();
        InboundTransactionItemProcessor inboundTransactionItemProcessor =
                new InboundTransactionItemProcessor(hpanStoreServiceMock,true);
        InboundTransaction inboundTransaction =
                inboundTransactionItemProcessor.process(getInboundTransaction());
        Assert.assertNotNull(inboundTransaction);
        InboundTransaction expectedTransaction = getInboundTransaction();
        expectedTransaction.setPan(DigestUtils.sha256Hex(expectedTransaction.getPan()));
        Assert.assertEquals(expectedTransaction, inboundTransaction);
        BDDMockito.verify(hpanStoreServiceMock).hasHpan(Mockito.eq(hashPan));
    }

    @Test
    public void process_KO() {
        String hashPan = DigestUtils.sha256Hex("pan");
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(hpanStoreServiceMock).hasHpan(Mockito.eq(hashPan));
        BDDMockito.doReturn("").when(hpanStoreServiceMock).getSalt();
        InboundTransactionItemProcessor inboundTransactionItemProcessor =
                new InboundTransactionItemProcessor(hpanStoreServiceMock,true);
        expectedException.expect(Exception.class);
        inboundTransactionItemProcessor.process(getInboundTransaction());
        BDDMockito.verify(hpanStoreServiceMock).hasHpan(Mockito.eq(hashPan));
    }

    @Test
    public void process_OK_validForReturn() {
        try {
            BDDMockito.doReturn(true).when(hpanStoreServiceMock).hasHpan(Mockito.eq("pan"));
            BDDMockito.doReturn("testSalt").when(hpanStoreServiceMock).getSalt();
            InboundTransactionItemProcessor inboundTransactionItemProcessor =
                    new InboundTransactionItemProcessor(hpanStoreServiceMock, false);

            InboundTransaction inboundTransaction = getInboundTransaction();
            inboundTransaction.setOperationType("01");
            inboundTransaction.setIdTrxIssuer("");
            InboundTransaction outboundTransaction =
                    inboundTransactionItemProcessor.process(getInboundTransaction());
            Assert.assertNotNull(outboundTransaction);
            Assert.assertEquals(getInboundTransaction(), outboundTransaction);
            BDDMockito.verify(hpanStoreServiceMock).hasHpan(Mockito.eq("pan"));

            inboundTransaction = getInboundTransaction();
            inboundTransaction.setOperationType("01");
            inboundTransaction.setIdTrxIssuer(null);
            outboundTransaction = inboundTransactionItemProcessor.process(inboundTransaction);
            Assert.assertNotNull(outboundTransaction);
            Assert.assertEquals(getInboundTransaction(), outboundTransaction);
            BDDMockito.verify(hpanStoreServiceMock, Mockito.times(2)).hasHpan(Mockito.eq("pan"));

        } catch (Exception e) {
            Assert.fail();
            e.printStackTrace();
        }
    }

    protected InboundTransaction getInboundTransaction() {
        return InboundTransaction.builder()
                .idTrxAcquirer("1")
                .acquirerCode("001")
                .trxDate("2020-04-09T16:22:45.304Z")
                .amount(1000L)
                .operationType("00")
                .pan("pan")
                .merchantId("0")
                .circuitType("00")
                .mcc("813")
                .idTrxIssuer("0")
                .amountCurrency("833")
                .correlationId("1")
                .acquirerId("0")
                .terminalId("0")
                .bin("000001")
                .vat("12345678901")
                .build();
    }
}