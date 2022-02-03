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
    public void clearAllEmptiesDataStructures() {
        Assert.assertEquals(0, storeSet.size());
        storeSet.add("test");
        storeService.clearAll();
        Assert.assertEquals(0, storeSet.size());
        Assert.assertEquals("", storeService.getSalt());
    }

}