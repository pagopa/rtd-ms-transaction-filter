package it.gov.pagopa.rtd.transaction_filter.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

public class BinStoreServiceTest {

    public BinStoreServiceTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    private List<Pair<String,String>> storeSet;
    private List<BufferedWriter> bufferedWriterList;
    private BinStoreService binStoreService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        storeSet = new ArrayList<>();
        bufferedWriterList = new ArrayList<>();
        binStoreService = new BinStoreServiceImpl(bufferedWriterList, storeSet);
    }

    @Test
    public void hasBin_NoBin() {
        Assert.assertEquals(0, storeSet.size());
        Assert.assertFalse(binStoreService.hasBin("bin"));
    }

    @Test
    public void hasBin() {
        storeSet.add(Pair.of("bin","end"));
        Assert.assertEquals(1, storeSet.size());
        Assert.assertFalse(binStoreService.hasBin("wrongBin"));
        Assert.assertTrue(binStoreService.hasBin("bin"));
    }

    @Test
    public void store() {
        Assert.assertEquals(0, storeSet.size());
        binStoreService.store("bin","end");
        Assert.assertEquals(1, storeSet.size());
        Assert.assertTrue(storeSet.contains(Pair.of("bin","end")));
    }

    @Test
    public void store_KO() {
        Assert.assertEquals(0, storeSet.size());
        expectedException.expect(IllegalArgumentException.class);
        binStoreService.store(null,null);
        Assert.assertEquals(0, storeSet.size());
    }

    @Test
    public void hasBin_KO() {
        Assert.assertEquals(0, storeSet.size());
        expectedException.expect(AssertionError.class);
        binStoreService.hasBin(null);
    }

    @Test
    public void clearAll() {
        Assert.assertEquals(0, storeSet.size());
        storeSet.add(Pair.of("test","end"));
        binStoreService.clearAll();
        Assert.assertEquals(0, storeSet.size());
    }

}