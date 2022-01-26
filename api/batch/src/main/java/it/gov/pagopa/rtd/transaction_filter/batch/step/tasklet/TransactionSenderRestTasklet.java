package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import feign.FeignException;
import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SasResponse;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
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

    private HpanConnectorService hpanConnectorService;
    private Resource resource;
    private boolean taskletEnabled = false;
    private HpanRestClient.SasScope scope;

    @Override
    @SneakyThrows
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        if (taskletEnabled) {
            int maxAttempts = 3;
            int remainingAttempts = maxAttempts;
            int delay = 2;
            boolean uploadSucceeded = false;

            SasResponse sasResponse;
            while (!uploadSucceeded && remainingAttempts > 0) {
                try {
                    sasResponse = hpanConnectorService.getSasToken(scope);
                    hpanConnectorService.uploadFile(resource.getFile(), sasResponse.getSas(), sasResponse.getAuthorizedContainer());
                    uploadSucceeded = true;
                } catch (FeignException e) {
                    remainingAttempts -= 1;
                    if (remainingAttempts < 1) {
                        throw e;
                    }
                    delay = (int) Math.pow(delay, 2);
                    log.error(e.getMessage());
                    log.info("Retrying after " + delay + " seconds (remaining attempts: " + remainingAttempts + ")");
                    Thread.sleep(delay * 1000l);
                }
            }
        }
        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(resource, "resource must be not null");
    }

}
