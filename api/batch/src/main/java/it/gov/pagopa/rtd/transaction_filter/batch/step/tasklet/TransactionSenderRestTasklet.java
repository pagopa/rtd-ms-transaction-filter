package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SasResponse;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import lombok.Data;
import lombok.SneakyThrows;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

@Data
public class TransactionSenderRestTasklet implements Tasklet, InitializingBean {

    private HpanConnectorService hpanConnectorService;
    private Resource resource;
    private boolean taskletEnabled = false;
    private HpanRestClient.SasScope scope;

    @Override
    @SneakyThrows
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        if (taskletEnabled) {
            SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
            retryPolicy.setMaxAttempts(3);
            ExponentialBackOffPolicy backoffPolicy = new ExponentialBackOffPolicy();
            backoffPolicy.setInitialInterval(3000l);

            RetryTemplate createSasTemplate = new RetryTemplate();
            createSasTemplate.setRetryPolicy(retryPolicy);
            createSasTemplate.setBackOffPolicy(backoffPolicy);
            SasResponse sasResponse = createSasTemplate.execute((RetryCallback<SasResponse, Exception>) context -> hpanConnectorService.getSasToken(scope));

            RetryTemplate putFileTemplate = new RetryTemplate();
            putFileTemplate.setRetryPolicy(retryPolicy);
            putFileTemplate.setBackOffPolicy(backoffPolicy);
            putFileTemplate.execute((RetryCallback<Void, Exception>) context -> hpanConnectorService.uploadFile(resource.getFile(), sasResponse.getSas(), sasResponse.getAuthorizedContainer()));
        }
        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(resource, "resource must be not null");
    }

}
