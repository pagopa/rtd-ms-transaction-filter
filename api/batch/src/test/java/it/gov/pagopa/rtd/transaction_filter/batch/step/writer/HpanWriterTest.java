package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import eu.sia.meda.BaseTest;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;

import java.util.Collections;

public class HpanWriterTest extends BaseTest {

    @Mock
    private HpanStoreService hpanStoreServiceMock;

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
            HpanWriter hpanWriter = new HpanWriter(this.hpanStoreServiceMock, false);
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
        HpanWriter hpanWriter = new HpanWriter(this.hpanStoreServiceMock, false);
        hpanWriter.write(Collections.singletonList("pan"));
        BDDMockito.verify(hpanStoreServiceMock).store(Mockito.eq("pan"));
    }

    @Test
    public void write_OK_MonoList_HashWithSalt() {
        BDDMockito.doNothing().when(hpanStoreServiceMock).store(Mockito.eq("pan"));
        HpanWriter hpanWriter = new HpanWriter(this.hpanStoreServiceMock, true);
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();
        stepExecution.getJobExecution().getExecutionContext().put("salt", "testSalt");
        hpanWriter.recoverSalt(stepExecution);
        hpanWriter.write(Collections.singletonList("pan"));
        BDDMockito.verify(hpanStoreServiceMock).store(Mockito.eq(DigestUtils.sha256Hex("pan"+"testSalt")));
    }

    @Test
    public void write_OK_MonoList_HashWithoutSalt() {
        BDDMockito.doNothing().when(hpanStoreServiceMock).store(Mockito.eq("pan"));
        HpanWriter hpanWriter = new HpanWriter(this.hpanStoreServiceMock, true);
        hpanWriter.write(Collections.singletonList("pan"));
        BDDMockito.verify(hpanStoreServiceMock).store(Mockito.eq(DigestUtils.sha256Hex("pan")));
    }

    @Test
    public void write_OK_MultiList_HashWithoutSalt() {
        try {
            BDDMockito.doNothing().when(hpanStoreServiceMock).store(Mockito.eq("pan"));
            HpanWriter hpanWriter = new HpanWriter(this.hpanStoreServiceMock, true);
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
        HpanWriter hpanWriter = new HpanWriter(this.hpanStoreServiceMock, true);
        expectedException.expect(NullPointerException.class);
        hpanWriter.write(null);
        BDDMockito.verifyZeroInteractions(hpanStoreServiceMock);
    }

}