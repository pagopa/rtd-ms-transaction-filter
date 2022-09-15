package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SasResponse;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import java.io.IOException;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

@Data
@Slf4j
public class TransactionSenderRestTasklet implements Tasklet, InitializingBean {

    private static final int RETRY_MAX_ATTEMPTS = 3;
    private int initialDelayInSeconds = 2;

    private HpanConnectorService hpanConnectorService;
    private Resource resource;
    private HpanRestClient.SasScope scope;
    private boolean taskletEnabled = false;

    @Override
    @SneakyThrows
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws IOException {
        if (taskletEnabled) {
            int remainingAttempts = RETRY_MAX_ATTEMPTS;
            int delay = initialDelayInSeconds;
            boolean uploadSucceeded = false;

            SasResponse sasResponse;
            while (!uploadSucceeded) {
                try {
                    sasResponse = hpanConnectorService.getSasToken(scope);
                    hpanConnectorService.uploadFile(resource.getFile(), sasResponse.getSas(), sasResponse.getAuthorizedContainer());
                    uploadSucceeded = true;
                } catch (IOException e) {
                    remainingAttempts -= 1;
                    if (remainingAttempts < 1) {
                        throw e;
                    }
                    delay = (int) Math.pow(delay, 2);
                    log.error(e.getMessage());
                    log.info("Retrying after " + delay + " seconds (remaining attempts: " + remainingAttempts + ")");
                    Thread.sleep(delay * 1000L);
                }
            }
        }
        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(hpanConnectorService, "hpanConnectorService must be not null");
        Assert.notNull(resource, "resource must be not null");
        Assert.notNull(scope, "scope must be not null");
    }

}
