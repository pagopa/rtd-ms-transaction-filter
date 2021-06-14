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

public class ParStoreServiceTest {

    public ParStoreServiceTest(){
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
    private ParStoreService parStoreService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        storeSet = new TreeSet<>();
        bufferedWriterList = new ArrayList<>();
        parStoreService = new ParStoreServiceImpl(bufferedWriterList, storeSet);
    }

    @Test
    public void hasPar_NoPar() {
        Assert.assertEquals(0, storeSet.size());
        Assert.assertFalse(parStoreService.hasPar("par"));
    }

    @Test
    public void hasPar() {
        storeSet.add("par");
        Assert.assertEquals(1, storeSet.size());
        Assert.assertFalse(parStoreService.hasPar("wrongPar"));
        Assert.assertTrue(parStoreService.hasPar("par"));
    }

    @Test
    public void store() {
        Assert.assertEquals(0, storeSet.size());
        parStoreService.store("par");
        Assert.assertEquals(1, storeSet.size());
        Assert.assertTrue(storeSet.contains("par"));
    }

    @Test
    public void store_KO() {
        Assert.assertEquals(0, storeSet.size());
        expectedException.expect(NullPointerException.class);
        parStoreService.store(null);
        Assert.assertEquals(0, storeSet.size());
    }

    @Test
    public void hasBin_KO() {
        Assert.assertEquals(0, storeSet.size());
        expectedException.expect(NullPointerException.class);
        parStoreService.hasPar(null);
    }

    @Test
    public void clearAll() {
        Assert.assertEquals(0, storeSet.size());
        storeSet.add("test");
        parStoreService.clearAll();
        Assert.assertEquals(0, storeSet.size());
    }

}