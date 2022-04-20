package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.batch.model.AdeTransactionsAggregate;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import it.gov.pagopa.rtd.transaction_filter.service.store.AccountingDateFlyweight;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerCodeFlyweight;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerIdFlyweight;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationData;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationKey;
import it.gov.pagopa.rtd.transaction_filter.service.store.CurrencyFlyweight;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;


public class TransactionAggregationWriterProcessorTest {

    private final static String TRANSMISSION_DATE = "2022-04-20";
    private AggregationKey key;
    private AggregationData data;
    private TransactionAggregationWriterProcessor processor;

    public TransactionAggregationWriterProcessorTest() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Mock
    private StoreService storeServiceMock;

    @Before
    public void setUp() {
        Mockito.reset(storeServiceMock);

        key = new AggregationKey();
        key.setTerminalId("1");
        key.setMerchantId("1");
        key.setAcquirerId(AcquirerIdFlyweight.createAcquirerId("1"));
        key.setAcquirerCode(AcquirerCodeFlyweight.createAcquirerCode("code"));
        key.setFiscalCode("FC");
        key.setAccountingDate(AccountingDateFlyweight.createAccountingDate("2022-04-07"));
        key.setOperationType((byte)0);

        data = new AggregationData();
        data.setCurrency(CurrencyFlyweight.createCurrency("978"));
        data.setVat("123882312");
        data.setNumTrx(3);
        data.setTotalAmount(10000);
        data.setPosType((byte)1);

        processor = new TransactionAggregationWriterProcessor(storeServiceMock, TRANSMISSION_DATE);
    }

    @Test
    public void processShouldReturnExpectedData() {
        BDDMockito.doReturn(data).when(storeServiceMock).getAggregate(key);

        AdeTransactionsAggregate aggregate = processor.process(key);

        AdeTransactionsAggregate expectedAggregate = new AdeTransactionsAggregate();
        expectedAggregate.setAccountingDate("2022-04-07");
        expectedAggregate.setAcquirerCode("code");
        expectedAggregate.setAcquirerId("1");
        expectedAggregate.setMerchantId("1");
        expectedAggregate.setTerminalId("1");
        expectedAggregate.setFiscalCode("FC");
        expectedAggregate.setCurrency("978");
        expectedAggregate.setPosType("01");
        expectedAggregate.setOperationType("00");
        expectedAggregate.setNumTrx(3);
        expectedAggregate.setTotalAmount(10000L);
        expectedAggregate.setTransmissionDate(TRANSMISSION_DATE);
        expectedAggregate.setVat("123882312");

        Assert.assertEquals(expectedAggregate, aggregate);
    }

    @Test
    public void processShouldHandleDirtyPosType() {
        data.setPosType(AggregationData.DIRTY_POS_TYPE);
        BDDMockito.doReturn(data).when(storeServiceMock).getAggregate(key);

        AdeTransactionsAggregate aggregate = processor.process(key);

        AdeTransactionsAggregate expectedAggregate = new AdeTransactionsAggregate();
        expectedAggregate.setAccountingDate("2022-04-07");
        expectedAggregate.setAcquirerCode("code");
        expectedAggregate.setAcquirerId("1");
        expectedAggregate.setMerchantId("1");
        expectedAggregate.setTerminalId("1");
        expectedAggregate.setFiscalCode("FC");
        expectedAggregate.setCurrency("978");
        expectedAggregate.setPosType("99");
        expectedAggregate.setOperationType("00");
        expectedAggregate.setNumTrx(3);
        expectedAggregate.setTotalAmount(10000L);
        expectedAggregate.setTransmissionDate(TRANSMISSION_DATE);
        expectedAggregate.setVat("123882312");

        Assert.assertEquals(expectedAggregate, aggregate);
    }

    @Test
    public void processShouldHandleDirtyCurrency() {
        data.setCurrency(CurrencyFlyweight.createCurrency(AggregationData.DIRTY_CURRENCY));
        BDDMockito.doReturn(data).when(storeServiceMock).getAggregate(key);

        AdeTransactionsAggregate aggregate = processor.process(key);

        AdeTransactionsAggregate expectedAggregate = new AdeTransactionsAggregate();
        expectedAggregate.setAccountingDate("2022-04-07");
        expectedAggregate.setAcquirerCode("code");
        expectedAggregate.setAcquirerId("1");
        expectedAggregate.setMerchantId("1");
        expectedAggregate.setTerminalId("1");
        expectedAggregate.setFiscalCode("FC");
        expectedAggregate.setCurrency(AggregationData.DIRTY_CURRENCY);
        expectedAggregate.setPosType("01");
        expectedAggregate.setOperationType("00");
        expectedAggregate.setNumTrx(3);
        expectedAggregate.setTotalAmount(10000L);
        expectedAggregate.setTransmissionDate(TRANSMISSION_DATE);
        expectedAggregate.setVat("123882312");

        Assert.assertEquals(expectedAggregate, aggregate);
    }

    @Test
    public void processShouldHandleDirtyVat() {
        data.setVat(AggregationData.DIRTY_VAT);
        BDDMockito.doReturn(data).when(storeServiceMock).getAggregate(key);

        AdeTransactionsAggregate aggregate = processor.process(key);

        AdeTransactionsAggregate expectedAggregate = new AdeTransactionsAggregate();
        expectedAggregate.setAccountingDate("2022-04-07");
        expectedAggregate.setAcquirerCode("code");
        expectedAggregate.setAcquirerId("1");
        expectedAggregate.setMerchantId("1");
        expectedAggregate.setTerminalId("1");
        expectedAggregate.setFiscalCode("FC");
        expectedAggregate.setCurrency("978");
        expectedAggregate.setPosType("01");
        expectedAggregate.setOperationType("00");
        expectedAggregate.setNumTrx(3);
        expectedAggregate.setTotalAmount(10000L);
        expectedAggregate.setTransmissionDate(TRANSMISSION_DATE);
        expectedAggregate.setVat(AggregationData.DIRTY_VAT);

        Assert.assertEquals(expectedAggregate, aggregate);
    }

}