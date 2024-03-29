package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;

public class HpanWriterTest {

    public HpanWriterTest(){
        MockitoAnnotations.initMocks(this);
    }

    @BeforeClass
    public static void configTest() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        ((Logger)LoggerFactory.getLogger("eu.sia")).setLevel(Level.DEBUG);
    }

    @Mock
    private StoreService storeServiceMock;

    @Before
    public void setUp() {
        Mockito.reset(storeServiceMock);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void write_OK_Empty() {
        try {
            BDDMockito.doNothing().when(storeServiceMock).store("pan");
            BDDMockito.doReturn("testSalt").when(storeServiceMock).getSalt();
            HpanWriter hpanWriter = new HpanWriter(this.storeServiceMock, false);
            hpanWriter.write(Chunk.of());
            BDDMockito.verifyNoInteractions(storeServiceMock);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void write_OK_MonoList_NoHash() {
        BDDMockito.doNothing().when(storeServiceMock).store("pan");
        BDDMockito.doReturn("").when(storeServiceMock).getSalt();
        HpanWriter hpanWriter = new HpanWriter(this.storeServiceMock, false);
        hpanWriter.write(Chunk.of("pan"));
        BDDMockito.verify(storeServiceMock).store("pan");
    }

    @Test
    public void write_OK_MonoList_HashWithSalt() {
        BDDMockito.doNothing().when(storeServiceMock).store("pan");
        BDDMockito.doReturn("testSalt").when(storeServiceMock).getSalt();
        HpanWriter hpanWriter = new HpanWriter(this.storeServiceMock, true);
        hpanWriter.write(Chunk.of("pan"));
        BDDMockito.verify(storeServiceMock).store(DigestUtils.sha256Hex("pan"+"testSalt"));
    }

    @Test
    public void write_OK_MonoList_HashWithoutSalt() {
        BDDMockito.doNothing().when(storeServiceMock).store("pan");
        BDDMockito.doReturn("").when(storeServiceMock).getSalt();
        HpanWriter hpanWriter = new HpanWriter(this.storeServiceMock, true);
        hpanWriter.write(Chunk.of("pan"));
        BDDMockito.verify(storeServiceMock).store(DigestUtils.sha256Hex("pan"));
    }

    @Test
    public void write_OK_MultiList_HashWithoutSalt() {
        try {
            BDDMockito.doNothing().when(storeServiceMock).store("pan");
            BDDMockito.doReturn("").when(storeServiceMock).getSalt();
            HpanWriter hpanWriter = new HpanWriter(this.storeServiceMock, true);
            hpanWriter.write(Chunk.of("pan", "pan", "pan", "pan", "pan"));
            BDDMockito.verify(storeServiceMock, Mockito.times(5))
                    .store(DigestUtils.sha256Hex("pan"));
        } catch (Exception e) {
            Assert.fail();
            e.printStackTrace();
        }
    }

}