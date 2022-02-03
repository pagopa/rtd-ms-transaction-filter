package it.gov.pagopa.rtd.transaction_filter.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.Assert;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.util.TreeSet;

public class HpanStoreServiceTest {

    private TreeSet<String> storeSet;
    private HpanStoreService hpanStoreService;

    public HpanStoreServiceTest() {
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
        hpanStoreService = new HpanStoreServiceImpl(storeSet);
    }

    @Test
    public void hasHpanReturnsTrueWhenPreviouslyStored() {
        String hpan = "SyIC2A1MZNoXLd1I";
        hpanStoreService.store(hpan);
        Assert.assertTrue(hpanStoreService.hasHpan(hpan));
    }

    @Test
    public void hasHpanReturnsFalseWhenMissing() {
        String hpan = "SyIC2A1MZNoXLd1I";
        Assert.assertFalse(hpanStoreService.hasHpan(hpan));
    }

    @Test
    public void hasHpanNullThrowsNullPointerException() {
        expectedException.expect(NullPointerException.class);
        hpanStoreService.hasHpan(null);
    }

    @Test
    public void storeHpanNullThrowsNullPointerException() {
        expectedException.expect(NullPointerException.class);
        hpanStoreService.store(null);
    }

    @Test
    public void getSaltBeforeStoreSaltReturnsEmptyString() {
        Assert.assertEquals("", hpanStoreService.getSalt());
    }

    @Test
    public void getSaltAfterStoreSaltReturnsStoredValue() {
        String salt = "11SALT555";
        hpanStoreService.storeSalt(salt);
        Assert.assertEquals(salt, hpanStoreService.getSalt());
    }

    @Test
    public void getKeyBeforeStoreSaltReturnsNull() {
        Assert.assertNull(hpanStoreService.getKey("keyName"));
    }

    @Test
    public void getKeyAfterStoreSaltReturnStoredValue() {
        hpanStoreService.storeKey("keyName", "keyValue");
        Assert.assertEquals("keyValue", hpanStoreService.getKey("keyName"));
    }

    @Test
    public void clearAllEmptiesDataStructures() {
        Assert.assertEquals(0, storeSet.size());
        storeSet.add("test");
        hpanStoreService.clearAll();
        Assert.assertEquals(0, storeSet.size());
        Assert.assertEquals("", hpanStoreService.getSalt());
    }

}