package it.gov.pagopa.rtd.transaction_filter.batch;

import it.gov.pagopa.rtd.transaction_filter.batch.config.TestConfig;
import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
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
import java.util.Date;

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

/**
 * Class for testing the CsvTransactionReaderBatch class
 */
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
        FeignAutoConfiguration.class
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
                "batchConfiguration.TransactionFilterBatch.transactionFilter.transactionDirectoryPath=classpath:/test-encrypt/**/transactions/*trx*.csv",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.outputDirectoryPath=classpath:/test-encrypt/output",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.publicKeyPath=classpath:/test-encrypt/publicKey.asc",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.applyHashing=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.timestampPattern=MM/dd/yyyy HH:mm:ss",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.applyEncrypt=true",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.saveHashing=true",
                "batchConfiguration.CsvTransactionReaderBatch.successArchivePath=classpath:/test-encrypt/**/success",
                "batchConfiguration.CsvTransactionReaderBatch.errorArchivePath=classpath:/test-encrypt/**/error",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.deleteLocalFiles=false",
                "batchConfiguration.TransactionFilterBatch.saltRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.hpanListRecovery.enabled=false",
                "batchConfiguration.TransactionFilterBatch.transactionSender.enabled=false",
                "batchConfiguration.TransactionFilterBatch.transactionFilter.readers.listener.enableAfterProcessFileLogging=false"
        }
)
public class TransactionFilterBatchTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @SpyBean
    HpanStoreService hpanStoreServiceSpy;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @SneakyThrows
    @Before
    public void setUp() {
        Mockito.reset(hpanStoreServiceSpy);
    }

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private JobParameters defaultJobParameters() {
        return new JobParametersBuilder()
                .addDate("startDateTime",  new Date())
                .toJobParameters();
    }


    @SneakyThrows
    @Test
    public void panReaderStep_testCoreSteps_OK() {

        tempFolder.newFolder("hpan");
        File panPgp = tempFolder.newFile("hpan/pan.pgp");

        FileOutputStream panPgpFOS = new FileOutputStream(panPgp);

        EncryptUtil.encryptFile(panPgpFOS,
                this.getClass().getResource("/test-encrypt/pan").getFile() + "/pan.csv",
                EncryptUtil.readPublicKey(
                        this.getClass().getResourceAsStream("/test-encrypt/publicKey.asc")),
                false,false);

        panPgpFOS.close();

        File file = new File(resolver.getResource("classpath:/test-encrypt/output")
                .getFile().getAbsolutePath()+"/test-trx.csv");

        if (!file.exists()) {
            file.createNewFile();
        }

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(defaultJobParameters());
        Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());

        BDDMockito.verify(hpanStoreServiceSpy, Mockito.times(3)).store(Mockito.any());
//        BDDMockito.verify(hpanStoreServiceSpy, Mockito.times(3)).hasHpan(Mockito.any());

        Assert.assertEquals(1,
                FileUtils.listFiles(
                        resolver.getResources("classpath:/test-encrypt/output")[0].getFile(),
                        new String[]{"pgp"},false).size());

    }

    @SneakyThrows
    @Test
    public void panReaderStep_testCoreSteps_KO() {

        tempFolder.newFolder("hpan");
        File panPgp = tempFolder.newFile("hpan/pan.pgp");

        FileOutputStream panPgpFOS = new FileOutputStream(panPgp);

        EncryptUtil.encryptFile(panPgpFOS,
                this.getClass().getResource("/test-encrypt/pan").getFile() + "/pan.csv",
                EncryptUtil.readPublicKey(
                        this.getClass().getResourceAsStream("/test-encrypt/otherPublicKey.asc")),
                false,false);

        panPgpFOS.close();

        jobLauncherTestUtils.launchStep("hpan-recovery-master-step");
        BDDMockito.verify(hpanStoreServiceSpy, Mockito.times(0)).store(Mockito.any());

    }

    @SneakyThrows
    @After
    public void tearDown() {
        Resource[] resources = resolver.getResources("classpath:/test-encrypt/output/*.pgp");
        if (resources.length > 1) {
            for (Resource resource : resources) {
                resource.getFile().delete();
            }
        }
        tempFolder.delete();
    }

}