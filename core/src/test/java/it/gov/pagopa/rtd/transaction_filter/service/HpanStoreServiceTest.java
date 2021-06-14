package it.gov.pagopa.rtd.transaction_filter.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class HpanStoreServiceTest {

    public HpanStoreServiceTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    private TreeSet<String> storeSet;
    private List<BufferedWriter> bufferedWriterList;
    private HpanStoreService hpanStoreService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        storeSet = new TreeSet<>();
        bufferedWriterList = new ArrayList<>();
        hpanStoreService = new HpanStoreServiceImpl(bufferedWriterList, storeSet);
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
    public void storeSalt_OK() {
        Assert.assertEquals("", hpanStoreService.getSalt());

    }

    @Test
    public void clearAll() {
        Assert.assertEquals(0, storeSet.size());
        storeSet.add("test");
        hpanStoreService.clearAll();
        Assert.assertEquals(0, storeSet.size());
    }

}