package it.gov.pagopa.rtd.transaction_filter.batch;

import it.gov.pagopa.rtd.transaction_filter.batch.config.TestConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import lombok.SneakyThrows;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.Rule;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import java.util.Date;
import org.mockito.Mockito;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@RunWith(SpringRunner.class)
@SpringBatchTest
@EnableAutoConfiguration
@DataJpaTest
@Transactional(propagation = NOT_SUPPORTED)
@Sql({
        "classpath:org/springframework/batch/core/schema-drop-hsqldb.sql",
        "classpath:org/springframework/batch/core/schema-hsqldb.sql"})
@ContextConfiguration(classes = {
        TestConfig.class,
        JacksonAutoConfiguration.class,
        TransactionFilterBatch.class,
        FeignAutoConfiguration.class,
        BatchExecutor.class
})
@TestPropertySource(
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "batchConfiguration.TransactionFilterBatch.panList.secretKeyPath=classpath:/test-encrypt/secretKey.asc",
                "batchConfiguration.TransactionFilterBatch.panList.passphrase=test",
                "batchConfiguration.TransactionFilterBatch.panList.skipLimit=0",
                "batchConfiguration.TransactionFilterBatch.panList.hpanDirectoryPath=classpath:/test-encrypt/**/hpan/*pan*.pgp",
                "batchConfiguration.TransactionFilterBatch.panList.linesToSkip=0",
                "batchConfiguration.TransactionFilterBatch.panList.applyDecrypt=true",
                "batchConfiguration.TransactionFilterBatch.panList.applyHashing=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath=classpath:/test-encrypt/**/transactions_wrong_filename/",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath=classpath:/test-encrypt/output",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.transactionLogsPath=classpath:/test-encrypt/errorLogs",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.publicKeyPath=classpath:/test-encrypt/publicKey.asc",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.applyHashing=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.timestampPattern=MM/dd/yyyy HH:mm:ss",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.applyEncrypt=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.saveHashing=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.deleteProcessedFiles=false",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.deleteOutputFiles=ERROR",
                "batchConfiguration.TransactionFilterBatch.successArchivePath=classpath:/test-encrypt/success",
                "batchConfiguration.TransactionFilterBatch.errorArchivePath=classpath:/test-encrypt/error",
                "batchConfiguration.TransactionFilterBatch.saltRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.abiToFiscalCodeMapRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.transactionSenderAde.enabled=false",
                "batchConfiguration.TransactionFilterBatch.transactionSenderRtd.enabled=false",
                "batchConfiguration.TransactionFilterBatch.senderAdeAckFilesRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.fileReportRecovery.directoryPath=classpath:/test-encrypt/reports",
                "batchConfiguration.TransactionFilterBatch.fileReportRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessFileLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnReadErrorLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnReadErrorFileLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessFileLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnProcessErrorLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnProcessErrorFileLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnWriteErrorLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableOnWriteErrorFileLogging=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.loggingFrequency=100",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.writerPoolSize=5"
        }
)
public class TransactionFilterBatchWrongInputTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private BatchExecutor batchExecutor;

    @SpyBean
    StoreService hpanStoreServiceSpy;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @SneakyThrows
    @Before
    public void setUp() {
        Mockito.reset(hpanStoreServiceSpy);

        deleteFiles("classpath:/test-encrypt/errorLogs/*.csv");
        deleteFiles("classpath:/test-encrypt/output/*.pgp");
        deleteFiles("classpath:/test-encrypt/output/*.csv");
    }

    @SneakyThrows
    @After
    public void tearDown() {
        tempFolder.delete();
    }

    @SneakyThrows
    private void deleteFiles(String classpath) {
        Resource[] resources = resolver.getResources(classpath);
        for (Resource resource : resources) {
            resource.getFile().delete();
        }
    }

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private JobParameters defaultJobParameters() {
        return new JobParametersBuilder()
                .addDate("startDateTime", new Date())
                .toJobParameters();
    }

    @SneakyThrows
    @Test
    public void jobExecutionExitImmediatelyWhenMissingInputFiles() {
        tempFolder.newFolder("hpan");
        File panPgp = tempFolder.newFile("hpan/pan.pgp");

        FileOutputStream panPgpFOS = new FileOutputStream(panPgp);

        EncryptUtil.encryptFile(panPgpFOS,
                this.getClass().getResource("/test-encrypt/pan").getFile() + "/pan.csv",
                EncryptUtil.readPublicKey(
                        this.getClass().getResourceAsStream("/test-encrypt/publicKey.asc")),
                false, false);

        panPgpFOS.close();

        JobExecution jobExecution = batchExecutor.execute(new Date());
        Assert.assertNull(jobExecution);
    }
}