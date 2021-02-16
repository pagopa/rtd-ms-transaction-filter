package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.WriterTrackerService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import java.util.Collections;

public class HpanStoreWriterTest {

    public HpanStoreWriterTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    private HpanStoreService hpanStoreServiceMock;

    @Mock
    private WriterTrackerService writerTrackerServiceMock;

    @Before
    public void setUp() {
        Mockito.reset(hpanStoreServiceMock);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void write_OK_Empty() {
        try {
            BDDMockito.doNothing().when(hpanStoreServiceMock).store(Mockito.eq("pan"));
            BDDMockito.doReturn("testSalt").when(hpanStoreServiceMock).getSalt();
            HpanStoreWriter hpanWriter = new HpanStoreWriter(this.hpanStoreServiceMock, true);
            hpanWriter.write(Collections.emptyList());
            BDDMockito.verifyZeroInteractions(hpanStoreServiceMock);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void write_OK_MonoList_NoHash() {
        BDDMockito.doNothing().when(hpanStoreServiceMock).store(Mockito.eq("pan"));
        BDDMockito.doReturn("").when(hpanStoreServiceMock).getSalt();
        HpanStoreWriter hpanWriter = new HpanStoreWriter(this.hpanStoreServiceMock, false);
        hpanWriter.write(Collections.singletonList("pan"));
        BDDMockito.verify(hpanStoreServiceMock).store(Mockito.eq("pan"));
    }

    @Test
    public void write_OK_MonoList_HashWithSalt() {
        BDDMockito.doNothing().when(hpanStoreServiceMock).store(Mockito.eq("pan"));
        BDDMockito.doReturn("testSalt").when(hpanStoreServiceMock).getSalt();
        HpanStoreWriter hpanWriter = new HpanStoreWriter(this.hpanStoreServiceMock, true);
        hpanWriter.write(Collections.singletonList("pan"));
        BDDMockito.verify(hpanStoreServiceMock).store(Mockito.eq(DigestUtils.sha256Hex("pan"+"testSalt")));
    }

    @Test
    public void write_OK_MonoList_HashWithoutSalt() {
        BDDMockito.doNothing().when(hpanStoreServiceMock).store(Mockito.eq("pan"));
        BDDMockito.doReturn("").when(hpanStoreServiceMock).getSalt();
        HpanStoreWriter hpanWriter = new HpanStoreWriter(this.hpanStoreServiceMock, true);
        hpanWriter.write(Collections.singletonList("pan"));
        BDDMockito.verify(hpanStoreServiceMock).store(Mockito.eq(DigestUtils.sha256Hex("pan")));
    }

    @Test
    public void write_OK_MultiList_HashWithoutSalt() {
        try {
            BDDMockito.doNothing().when(hpanStoreServiceMock).store(Mockito.eq("pan"));
            BDDMockito.doReturn("").when(hpanStoreServiceMock).getSalt();
            HpanStoreWriter hpanWriter = new HpanStoreWriter(this.hpanStoreServiceMock, true);
            hpanWriter.write(Collections.nCopies(5,"pan"));
            BDDMockito.verify(hpanStoreServiceMock, Mockito.times(5))
                    .store(Mockito.eq(DigestUtils.sha256Hex("pan")));
        } catch (Exception e) {
            Assert.fail();
            e.printStackTrace();
        }
    }

    @Test
    public void write_KO_null() {
        HpanStoreWriter hpanWriter = new HpanStoreWriter(this.hpanStoreServiceMock,true);
        BDDMockito.doReturn("testSalt").when(hpanStoreServiceMock).getSalt();
        expectedException.expect(NullPointerException.class);
        hpanWriter.write(null);
        BDDMockito.verifyZeroInteractions(hpanStoreServiceMock);
    }

}