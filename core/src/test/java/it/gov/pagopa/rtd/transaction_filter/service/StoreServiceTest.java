package it.gov.pagopa.rtd.transaction_filter.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.store.AccountingDateFlyweight;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerCodeFlyweight;
import it.gov.pagopa.rtd.transaction_filter.service.store.AcquirerIdFlyweight;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationData;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationKey;
import it.gov.pagopa.rtd.transaction_filter.service.store.CurrencyFlyweight;
import java.util.HashSet;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.Assert;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.util.TreeSet;

public class StoreServiceTest {

    private TreeSet<String> storeSet;
    private StoreService storeService;

    public StoreServiceTest() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        storeSet = new TreeSet<>();
        storeService = new StoreServiceImpl(storeSet);
    }

    @Test
    public void hasHpanReturnsTrueWhenPreviouslyStored() {
        String hpan = "SyIC2A1MZNoXLd1I";
        storeService.store(hpan);
        Assert.assertTrue(storeService.hasHpan(hpan));
    }

    @Test
    public void hasHpanReturnsFalseWhenMissing() {
        String hpan = "SyIC2A1MZNoXLd1I";
        Assert.assertFalse(storeService.hasHpan(hpan));
    }

    @Test
    public void hasHpanNullThrowsNullPointerException() {
        expectedException.expect(NullPointerException.class);
        storeService.hasHpan(null);
    }

    @Test
    public void storeHpanNullThrowsNullPointerException() {
        expectedException.expect(NullPointerException.class);
        storeService.store(null);
    }

    @Test
    public void getSaltBeforeStoreSaltReturnsEmptyString() {
        Assert.assertEquals("", storeService.getSalt());
    }

    @Test
    public void getSaltAfterStoreSaltReturnsStoredValue() {
        String salt = "11SALT555";
        storeService.storeSalt(salt);
        Assert.assertEquals(salt, storeService.getSalt());
    }

    @Test
    public void getKeyBeforeStoreSaltReturnsNull() {
        Assert.assertNull(storeService.getKey("keyName"));
    }

    @Test
    public void getKeyAfterStoreSaltReturnStoredValue() {
        storeService.storeKey("keyName", "keyValue");
        Assert.assertEquals("keyValue", storeService.getKey("keyName"));
    }

    @Test
    public void getAggregateBeforeStoreReturnsNull() {
        AggregationKey key = new AggregationKey();
        key.setTerminalId("1");
        key.setMerchantId("1");
        key.setAcquirerId(AcquirerIdFlyweight.createAcquirerId("1"));
        key.setAcquirerCode(AcquirerCodeFlyweight.createAcquirerCode("code"));
        key.setFiscalCode("FC");
        key.setAccountingDate(AccountingDateFlyweight.createAccountingDate("2022-04-07"));
        key.setOperationType((byte)0);
        Assert.assertNull(storeService.getAggregate(key));
    }

    @Test
    public void getAggregateAfterStoreReturnsData() {
        AggregationKey key = new AggregationKey();
        key.setTerminalId("1");
        key.setMerchantId("1");
        key.setAcquirerId(AcquirerIdFlyweight.createAcquirerId("1"));
        key.setAcquirerCode(AcquirerCodeFlyweight.createAcquirerCode("code"));
        key.setFiscalCode("FC");
        key.setAccountingDate(AccountingDateFlyweight.createAccountingDate("2022-04-07"));
        key.setOperationType((byte)0);
        storeService.storeAggregate(key, 1000, "978", null, "01");
        AggregationData expectedData = new AggregationData();
        expectedData.setNumTrx((short)1);
        expectedData.setTotalAmount(1000);
        expectedData.setPosType((byte)1);
        expectedData.setVat(null);
        expectedData.setCurrency(CurrencyFlyweight.createCurrency("978"));
        Assert.assertEquals(expectedData.toString(), storeService.getAggregate(key).toString());
    }

    @Test
    public void getAggregateAfterMultipleStoreReturnsData() {
        AggregationKey key = new AggregationKey();
        key.setTerminalId("1");
        key.setMerchantId("1");
        key.setAcquirerId(AcquirerIdFlyweight.createAcquirerId("1"));
        key.setAcquirerCode(AcquirerCodeFlyweight.createAcquirerCode("code"));
        key.setFiscalCode("FC");
        key.setAccountingDate(AccountingDateFlyweight.createAccountingDate("2022-04-07"));
        key.setOperationType((byte)0);
        storeService.storeAggregate(key, 1000, "978", null, "01");
        storeService.storeAggregate(key, 2500, "978", null, "01");
        AggregationData expectedData = new AggregationData();
        expectedData.setNumTrx((short)2);
        expectedData.setTotalAmount(3500);
        expectedData.setPosType((byte)1);
        expectedData.setVat(null);
        expectedData.setCurrency(CurrencyFlyweight.createCurrency("978"));
        Assert.assertEquals(expectedData.toString(), storeService.getAggregate(key).toString());
    }

    @Test
    public void getAggregateKeySetReturnsExpectedKeys() {
        AggregationKey key = new AggregationKey();
        key.setTerminalId("1");
        key.setMerchantId("1");
        key.setAcquirerId(AcquirerIdFlyweight.createAcquirerId("1"));
        key.setAcquirerCode(AcquirerCodeFlyweight.createAcquirerCode("code"));
        key.setFiscalCode("FC");
        key.setAccountingDate(AccountingDateFlyweight.createAccountingDate("2022-04-07"));
        key.setOperationType((byte)0);
        storeService.storeAggregate(key, 1000, "978", null, "01");
        storeService.storeAggregate(key, 5000, "978", null, "00");
        Set<AggregationKey> expectedKeySet = new HashSet<>();
        expectedKeySet.add(key);
        Assert.assertEquals(expectedKeySet, storeService.getAggregateKeySet());
    }

    @Test
    public void clearAggregatesShouldEmptyKeySet() {
        AggregationKey key = new AggregationKey();
        key.setTerminalId("1");
        key.setMerchantId("1");
        key.setAcquirerId(AcquirerIdFlyweight.createAcquirerId("1"));
        key.setAcquirerCode(AcquirerCodeFlyweight.createAcquirerCode("code"));
        key.setFiscalCode("FC");
        key.setAccountingDate(AccountingDateFlyweight.createAccountingDate("2022-04-07"));
        key.setOperationType((byte)0);
        storeService.storeAggregate(key, 1000, "978", null, "01");
        storeService.clearAggregates();
        Assert.assertEquals(0, storeService.getAggregateKeySet().size());
    }

    @Test
    public void getHashBeforeStoreHashReturnsNull() {
        Assert.assertNull(storeService.getHash("fileName"));
    }

    @Test
    public void getHashAfterStoreHashReturnStoredValue() {
        String sha256hex = "089bae15036715a9e613552a0free186c6b610a85beb16cc4192595a940f16d3";
        storeService.storeHash("fileName", sha256hex);
        Assert.assertEquals(sha256hex, storeService.getHash("fileName"));
    }

    @Test
    public void clearAllEmptiesDataStructures() {
        Assert.assertEquals(0, storeSet.size());
        storeSet.add("test");
        storeService.clearAll();
        Assert.assertEquals(0, storeSet.size());
        Assert.assertEquals("", storeService.getSalt());
    }

}