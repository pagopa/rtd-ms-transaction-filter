package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;


public class InboundTransactionItemProcessorTest {

    private final String FAKE_ENROLLED_PAN = "pan123";
    private final String FAKE_SALT = "testSalt";

    public InboundTransactionItemProcessorTest() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    private StoreService storeServiceMock;

    @Before
    public void setUp() {
        Mockito.reset(storeServiceMock);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void processTransactionWithHashingDisabled() {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(Mockito.eq(FAKE_ENROLLED_PAN));
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction transaction = processor.process(fakeInboundTransaction());
        Assert.assertEquals(fakeInboundTransaction(), transaction);
        Mockito.verify(storeServiceMock, Mockito.times(1)).hasHpan(FAKE_ENROLLED_PAN);
        Mockito.verify(storeServiceMock, Mockito.times(1)).getSalt();
    }

    @Test
    public void processTransactionWithHashingEnabled() {
        String hashPan = DigestUtils.sha256Hex(FAKE_ENROLLED_PAN + FAKE_SALT);
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(Mockito.eq(hashPan));
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, true);
        InboundTransaction transaction = processor.process(fakeInboundTransaction());
        Assert.assertEquals(fakeInboundTransaction(), transaction);
        Mockito.verify(storeServiceMock, Mockito.times(1)).hasHpan(hashPan);
        Mockito.verify(storeServiceMock, Mockito.times(1)).getSalt();
    }

    @Test
    public void processTransactionPanNotMatching() {
        BDDMockito.doReturn(false).when(storeServiceMock).hasHpan(Mockito.eq(FAKE_ENROLLED_PAN));
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransactionItemProcessor processor = new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction outboundTransaction = processor.process(fakeInboundTransaction());
        Assert.assertNull(outboundTransaction);
    }

    @Test
    public void process_OK_SaveHashing() {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(Mockito.eq(FAKE_ENROLLED_PAN));
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransactionItemProcessor inboundTransactionItemProcessor =
                new InboundTransactionItemProcessor(storeServiceMock, false);
        InboundTransaction inboundTransaction =
                inboundTransactionItemProcessor.process(fakeInboundTransaction());
        Assert.assertNotNull(inboundTransaction);
        InboundTransaction expectedTransaction = fakeInboundTransaction();
        expectedTransaction.setPan(DigestUtils.sha256Hex(expectedTransaction.getPan() + FAKE_SALT));
        Assert.assertEquals(expectedTransaction, inboundTransaction);
        BDDMockito.verify(storeServiceMock).hasHpan(Mockito.eq(FAKE_ENROLLED_PAN));
    }

    @Test
    public void process_OK_AllHashing() {
        String hashPan = DigestUtils.sha256Hex(FAKE_ENROLLED_PAN + FAKE_SALT);
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(Mockito.eq(hashPan));
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransactionItemProcessor inboundTransactionItemProcessor =
                new InboundTransactionItemProcessor(storeServiceMock, true);
        InboundTransaction inboundTransaction =
                inboundTransactionItemProcessor.process(fakeInboundTransaction());
        Assert.assertNotNull(inboundTransaction);
        InboundTransaction expectedTransaction = fakeInboundTransaction();
        expectedTransaction.setPan(DigestUtils.sha256Hex(expectedTransaction.getPan() + FAKE_SALT));
        Assert.assertEquals(expectedTransaction, inboundTransaction);
        BDDMockito.verify(storeServiceMock).hasHpan(Mockito.eq(hashPan));
    }

    @Test
    public void process_OK_AllHashing_NoSalt() {
        String hashPan = DigestUtils.sha256Hex(FAKE_ENROLLED_PAN);
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(Mockito.eq(hashPan));
        BDDMockito.doReturn("").when(storeServiceMock).getSalt();
        InboundTransactionItemProcessor inboundTransactionItemProcessor =
                new InboundTransactionItemProcessor(storeServiceMock, true);
        InboundTransaction inboundTransaction =
                inboundTransactionItemProcessor.process(fakeInboundTransaction());
        Assert.assertNotNull(inboundTransaction);
        InboundTransaction expectedTransaction = fakeInboundTransaction();
        expectedTransaction.setPan(DigestUtils.sha256Hex(expectedTransaction.getPan()));
        Assert.assertEquals(expectedTransaction, inboundTransaction);
        BDDMockito.verify(storeServiceMock).hasHpan(Mockito.eq(hashPan));
    }

    @Test
    public void process_KO() {
        String hashPan = DigestUtils.sha256Hex(FAKE_ENROLLED_PAN);
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(storeServiceMock).hasHpan(Mockito.eq(hashPan));
        BDDMockito.doReturn("").when(storeServiceMock).getSalt();
        InboundTransactionItemProcessor inboundTransactionItemProcessor =
                new InboundTransactionItemProcessor(storeServiceMock, true);
        expectedException.expect(Exception.class);
        inboundTransactionItemProcessor.process(fakeInboundTransaction());
        BDDMockito.verify(storeServiceMock).hasHpan(Mockito.eq(hashPan));
    }

    @Test
    public void process_OK_validForReturn() {
        BDDMockito.doReturn(true).when(storeServiceMock).hasHpan(Mockito.eq(FAKE_ENROLLED_PAN));
        BDDMockito.doReturn(FAKE_SALT).when(storeServiceMock).getSalt();
        InboundTransactionItemProcessor inboundTransactionItemProcessor = new InboundTransactionItemProcessor(storeServiceMock, false);

        InboundTransaction inboundTransaction = fakeInboundTransaction();
        inboundTransaction.setOperationType("01");
        inboundTransaction.setIdTrxIssuer("A03");
        InboundTransaction outboundTransaction = inboundTransactionItemProcessor.process(fakeInboundTransaction());
        Assert.assertNotNull(outboundTransaction);
        Assert.assertEquals(fakeInboundTransaction(), outboundTransaction);
        BDDMockito.verify(storeServiceMock).hasHpan(Mockito.eq(FAKE_ENROLLED_PAN));

        inboundTransaction = fakeInboundTransaction();
        inboundTransaction.setOperationType("01");
        inboundTransaction.setIdTrxIssuer("02");
        outboundTransaction = inboundTransactionItemProcessor.process(inboundTransaction);
        Assert.assertNotNull(outboundTransaction);
        Assert.assertEquals(fakeInboundTransaction(), outboundTransaction);
        BDDMockito.verify(storeServiceMock, Mockito.times(2)).hasHpan(Mockito.eq(FAKE_ENROLLED_PAN));
    }

    private InboundTransaction fakeInboundTransaction() {
        return InboundTransaction.builder()
                .acquirerCode("001")
                .operationType("00")
                .circuitType("00")
                .pan(FAKE_ENROLLED_PAN)
                .trxDate("2020-04-09T16:22:45.304+00:00")
                .idTrxAcquirer("1")
                .idTrxIssuer("0")
                .correlationId("1")
                .amount(1000L)
                .amountCurrency("978")
                .acquirerId("0")
                .merchantId("0")
                .terminalId("0")
                .bin("000001")
                .mcc("813")
                .fiscalCode("fc123456")
                .vat("12345678901")
                .posType("00")
                .build();
    }
}