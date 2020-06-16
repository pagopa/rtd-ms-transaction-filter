package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import eu.sia.meda.BaseTest;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;

public class SaltRecoveryTaskletTest extends BaseTest {

    @Mock
    private HpanConnectorService hpanConnectorServiceMock;

    @SneakyThrows
    @Before
    public void setUp() {
        Mockito.reset(hpanConnectorServiceMock);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @SneakyThrows
    @Test
    public void testSalt_Ok() {
        BDDMockito.doReturn("testSalt").when(hpanConnectorServiceMock).getSalt();
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        SaltRecoveryTasklet saltRecoveryTasklet = new SaltRecoveryTasklet();
        saltRecoveryTasklet.setHpanConnectorService(hpanConnectorServiceMock);
        saltRecoveryTasklet.setTaskletEnabled(true);
        saltRecoveryTasklet.execute(new StepContribution(execution),chunkContext);
        BDDMockito.verify(hpanConnectorServiceMock).getSalt();
    }

    @SneakyThrows
    @Test
    public void testSalt_KO() {
        BDDMockito.doAnswer(invocationOnMock -> {
            throw new Exception();
        }).when(hpanConnectorServiceMock).getSalt();
        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        SaltRecoveryTasklet saltRecoveryTasklet = new SaltRecoveryTasklet();
        saltRecoveryTasklet.setHpanConnectorService(hpanConnectorServiceMock);
        saltRecoveryTasklet.setTaskletEnabled(true);
        expectedException.expect(Exception.class);
        saltRecoveryTasklet.execute(new StepContribution(execution),chunkContext);
        BDDMockito.verify(hpanConnectorServiceMock).getSalt();
    }

}