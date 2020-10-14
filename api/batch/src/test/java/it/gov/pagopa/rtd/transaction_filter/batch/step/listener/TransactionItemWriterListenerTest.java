package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class TransactionItemWriterListenerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @SneakyThrows
    @Test
    public void onWriteError_OK() {

        File folder = tempFolder.newFolder("testWriter");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemWriterListener transactionItemWriterListener = new TransactionItemWriterListener();
        transactionItemWriterListener.setExecutionDate(executionDate);
        transactionItemWriterListener.setEnableOnErrorLogging(true);
        transactionItemWriterListener.setEnableAfterWriteLogging(true);
        transactionItemWriterListener.setEnableOnErrorFileLogging(true);
        transactionItemWriterListener.setLoggingFrequency(1L);
        transactionItemWriterListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemWriterListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemWriterListener.onWriteError(new Exception(), Collections.singletonList(InboundTransaction
                .builder().filename("test").lineNumber(1).build()));

        Assert.assertEquals(1,
                FileUtils.listFiles(
                        resolver.getResources("classpath:/test-encrypt/**/testWriter")[0].getFile(),
                        new String[]{"csv"},false).size());

    }

    @SneakyThrows
    @Test
    public void onWriteError_OK_NoFileWritten() {

        File folder = tempFolder.newFolder("testWriter");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemWriterListener transactionItemWriterListener = new TransactionItemWriterListener();
        transactionItemWriterListener.setExecutionDate(executionDate);
        transactionItemWriterListener.setEnableOnErrorLogging(true);
        transactionItemWriterListener.setEnableAfterWriteLogging(true);
        transactionItemWriterListener.setEnableOnErrorFileLogging(false);
        transactionItemWriterListener.setLoggingFrequency(1L);
        transactionItemWriterListener.setResolver(new PathMatchingResourcePatternResolver());
        transactionItemWriterListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        transactionItemWriterListener.onWriteError(new Exception(), Collections.singletonList(InboundTransaction
                .builder().filename("test").lineNumber(1).build()));

        Assert.assertEquals(0,
                FileUtils.listFiles(
                        resolver.getResources("classpath:/test-encrypt/**/testWriter")[0].getFile(),
                        new String[]{"csv"},false).size());

    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}