package it.gov.pagopa.rtd.transaction_filter.service;

import eu.sia.meda.BaseTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.TreeSet;

public class HpanStoreServiceTest extends BaseTest {

    private TreeSet<String> storeSet;
    private HpanStoreService hpanStoreService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        storeSet = new TreeSet<>();
        hpanStoreService = new HpanStoreServiceImpl(storeSet);
    }

    @Test
    public void hasHpan_NoPan() {
        Assert.assertEquals(0, storeSet.size());
        Assert.assertFalse(hpanStoreService.hasHpan("pan"));
    }

    @Test
    public void hasHpan() {
        storeSet.add("pan");
        Assert.assertEquals(1, storeSet.size());
        Assert.assertFalse(hpanStoreService.hasHpan("wrongPan"));
        Assert.assertTrue(hpanStoreService.hasHpan("pan"));
    }

    @Test
    public void store() {
        Assert.assertEquals(0, storeSet.size());
        hpanStoreService.store("pan");
        Assert.assertEquals(1, storeSet.size());
        Assert.assertTrue(storeSet.contains("pan"));
    }

    @Test
    public void store_KO() {
        Assert.assertEquals(0, storeSet.size());
        expectedException.expect(NullPointerException.class);
        hpanStoreService.store(null);
        Assert.assertEquals(0, storeSet.size());
    }

    @Test
    public void hasHpan_KO() {
        Assert.assertEquals(0, storeSet.size());
        expectedException.expect(NullPointerException.class);
        hpanStoreService.hasHpan(null);
    }

    @Test
    public void clearAll() {
        Assert.assertEquals(0, storeSet.size());
        storeSet.add("test");
        hpanStoreService.clearAll();
        Assert.assertEquals(0, storeSet.size());
    }

}