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

public class TokenPanStoreServiceTest {

    public TokenPanStoreServiceTest(){
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
    private TokenPanStoreService tokenPanStoreService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        storeSet = new TreeSet<>();
        bufferedWriterList = new ArrayList<>();
        tokenPanStoreService = new TokenPanStoreServiceImpl(bufferedWriterList, storeSet);
    }

    @Test
    public void hasTokenPan_NoTokenPan() {
        Assert.assertEquals(0, storeSet.size());
        Assert.assertFalse(tokenPanStoreService.hasTokenPAN("tokenPan"));
    }

    @Test
    public void hasTokenPan() {
        storeSet.add("tokenPan");
        Assert.assertEquals(1, storeSet.size());
        Assert.assertFalse(tokenPanStoreService.hasTokenPAN("wrong_tokenPan"));
        Assert.assertTrue(tokenPanStoreService.hasTokenPAN("tokenPan"));
    }

    @Test
    public void store() {
        Assert.assertEquals(0, storeSet.size());
        tokenPanStoreService.store("tokenPan");
        Assert.assertEquals(1, storeSet.size());
        Assert.assertTrue(storeSet.contains("tokenPan"));
    }

    @Test
    public void store_KO() {
        Assert.assertEquals(0, storeSet.size());
        expectedException.expect(NullPointerException.class);
        tokenPanStoreService.store(null);
        Assert.assertEquals(0, storeSet.size());
    }

    @Test
    public void hasBin_KO() {
        Assert.assertEquals(0, storeSet.size());
        expectedException.expect(NullPointerException.class);
        tokenPanStoreService.hasTokenPAN(null);
    }

    @Test
    public void clearAll() {
        Assert.assertEquals(0, storeSet.size());
        storeSet.add("test");
        tokenPanStoreService.clearAll();
        Assert.assertEquals(0, storeSet.size());
    }

}