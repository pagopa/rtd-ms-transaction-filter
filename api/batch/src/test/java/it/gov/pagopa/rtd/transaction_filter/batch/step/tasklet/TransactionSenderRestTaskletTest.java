package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import static org.mockito.ArgumentMatchers.any;

import it.gov.pagopa.rtd.transaction_filter.connector.HpanRestClient;
import it.gov.pagopa.rtd.transaction_filter.connector.SasResponse;
import it.gov.pagopa.rtd.transaction_filter.service.HpanConnectorService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.core.io.UrlResource;

@Slf4j
class TransactionSenderRestTaskletTest {

    AutoCloseable closeable;

    @Mock
    private HpanConnectorService hpanConnectorServiceMock;

    @TempDir
    private Path tempFolder;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @SneakyThrows
    @Test
    void testExecuteTaskletWhenEnabled() {
        SasResponse sasResponseMock = new SasResponse();
        sasResponseMock.setSas("sas-token");
        sasResponseMock.setAuthorizedContainer("authorized-container");
        BDDMockito.doReturn(sasResponseMock).when(hpanConnectorServiceMock).getSasToken(HpanRestClient.SasScope.ADE);

        Path fileToSend = Files.createFile(tempFolder.resolve("test"));

        TransactionSenderRestTasklet transactionSenderRestTasklet = new TransactionSenderRestTasklet();
        transactionSenderRestTasklet.setTaskletEnabled(true);
        transactionSenderRestTasklet.setHpanConnectorService(this.hpanConnectorServiceMock);
        transactionSenderRestTasklet.setResource(new UrlResource("file:/" + fileToSend));
        transactionSenderRestTasklet.setScope(HpanRestClient.SasScope.ADE);

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        transactionSenderRestTasklet.execute(new StepContribution(execution), chunkContext);

        BDDMockito.verify(hpanConnectorServiceMock).getSasToken(HpanRestClient.SasScope.ADE);
        BDDMockito.verify(hpanConnectorServiceMock).uploadFile(fileToSend.toFile(), sasResponseMock.getSas(), sasResponseMock.getAuthorizedContainer());
    }

    @SneakyThrows
    @Test
    void testExecuteTaskletWhenDisabled() {
        Path fileToSend = Files.createFile(tempFolder.resolve("test"));

        TransactionSenderRestTasklet transactionSenderRestTasklet = new TransactionSenderRestTasklet();
        transactionSenderRestTasklet.setTaskletEnabled(false);
        transactionSenderRestTasklet.setHpanConnectorService(this.hpanConnectorServiceMock);
        transactionSenderRestTasklet.setResource(new UrlResource("file:/" + fileToSend));
        transactionSenderRestTasklet.setScope(HpanRestClient.SasScope.ADE);

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        transactionSenderRestTasklet.execute(new StepContribution(execution), chunkContext);

        BDDMockito.verify(hpanConnectorServiceMock, Mockito.times(0)).getSasToken(any());
        BDDMockito.verify(hpanConnectorServiceMock, Mockito.times(0)).uploadFile(any(), any(), any());
    }

    @SneakyThrows
    @Test
    void whenUploadFailsThenRetry() {
        Path fileToSend = Files.createFile(tempFolder.resolve("test"));
        SasResponse sasResponseMock = new SasResponse();
        sasResponseMock.setSas("sas-token");
        sasResponseMock.setAuthorizedContainer("authorized-container");
        BDDMockito.doReturn(sasResponseMock).when(hpanConnectorServiceMock).getSasToken(HpanRestClient.SasScope.ADE);
        BDDMockito.given(hpanConnectorServiceMock.uploadFile(any(), any(), any()))
            .willAnswer(invocation -> { throw new IOException("Upload failed!"); });

        TransactionSenderRestTasklet transactionSenderRestTasklet = new TransactionSenderRestTasklet();
        transactionSenderRestTasklet.setTaskletEnabled(true);
        transactionSenderRestTasklet.setHpanConnectorService(this.hpanConnectorServiceMock);
        transactionSenderRestTasklet.setResource(new UrlResource("file:/" + fileToSend));
        transactionSenderRestTasklet.setScope(HpanRestClient.SasScope.ADE);
        transactionSenderRestTasklet.setInitialDelayInSeconds(0);

        StepExecution execution = MetaDataInstanceFactory.createStepExecution();
        StepContext stepContext = new StepContext(execution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        // after three retries the exception will be thrown out anyway
        Assert.assertThrows(IOException.class, () -> transactionSenderRestTasklet.execute(new StepContribution(execution), chunkContext));

        BDDMockito.verify(hpanConnectorServiceMock, Mockito.times(3)).uploadFile(any(), any(), any());
    }

    @SneakyThrows
    @AfterEach
    public void tearDown() {
        closeable.close();
    }

}