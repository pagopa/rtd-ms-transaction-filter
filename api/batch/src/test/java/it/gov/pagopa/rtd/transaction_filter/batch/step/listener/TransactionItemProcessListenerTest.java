package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.config.LoggerRule;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class TransactionItemProcessListenerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @Rule
    public final LoggerRule loggerRule = new LoggerRule();

    @SneakyThrows
    @Test
    public void afterProcess_OK() {

        File folder = tempFolder.newFolder("testProcess");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemProcessListener transactionItemProcessListener = new TransactionItemProcessListener();
        transactionItemProcessListener.setExecutionDate(executionDate);
        transactionItemProcessListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemProcessListener.setEnableOnErrorLogging(true);
        transactionItemProcessListener.setEnableAfterProcessLogging(true);
        transactionItemProcessListener.setEnableAfterProcessFileLogging(true);
        transactionItemProcessListener.setEnableOnErrorFileLogging(true);
        transactionItemProcessListener.setEnableAfterProcessLogging(true);
        transactionItemProcessListener.setLoggingFrequency(1L);
        transactionItemProcessListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemProcessListener.afterProcess(
                InboundTransaction.builder().filename("test").lineNumber(1).build(),
                null);

    }

    @SneakyThrows
    @Test
    public void onProcessError_OK() {

        File folder = tempFolder.newFolder("testProcess");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemProcessListener transactionItemProcessListener = new TransactionItemProcessListener();
        transactionItemProcessListener.setExecutionDate(executionDate);
        transactionItemProcessListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemProcessListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemProcessListener.setEnableOnErrorLogging(true);
        transactionItemProcessListener.setEnableAfterProcessLogging(true);
        transactionItemProcessListener.setEnableOnErrorFileLogging(true);
        transactionItemProcessListener.setLoggingFrequency(1L);
        transactionItemProcessListener.onProcessError(
                InboundTransaction.builder().filename("test").lineNumber(1).build(),
                new Exception());

        Assert.assertEquals(1,
                FileUtils.listFiles(
                        resolver.getResources("classpath:/test-encrypt/**/testProcess")[0].getFile(),
                        new String[]{"csv"}, false).size());

    }


    @SneakyThrows
    @Test
    public void onProcessError_OK_NoFileWritten() {

        File folder = tempFolder.newFolder("testProcess");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemProcessListener transactionItemProcessListener = new TransactionItemProcessListener();
        transactionItemProcessListener.setExecutionDate(executionDate);
        transactionItemProcessListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemProcessListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemProcessListener.setEnableOnErrorLogging(true);
        transactionItemProcessListener.setEnableAfterProcessLogging(true);
        transactionItemProcessListener.setEnableOnErrorFileLogging(false);
        transactionItemProcessListener.setLoggingFrequency(1L);
        transactionItemProcessListener.onProcessError(
                InboundTransaction.builder().filename("test").lineNumber(1).build(),
                new Exception());

        Assert.assertEquals(0,
                FileUtils.listFiles(
                        resolver.getResources("classpath:/test-encrypt/**/testProcess")[0].getFile(),
                        new String[]{"csv"}, false).size());

    }


    @After
    public void tearDown() {
        tempFolder.delete();
    }

}