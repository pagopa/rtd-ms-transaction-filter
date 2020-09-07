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

public class TransactionItemProcessListenerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));

    @SneakyThrows
    @Test
    public void afterProcess_OK() {

        File folder = tempFolder.newFolder("testProcess");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemProcessListener TransactionItemProcessListener = new TransactionItemProcessListener();
        TransactionItemProcessListener.setExecutionDate(executionDate);
        TransactionItemProcessListener.setResolver(new PathMatchingResourcePatternResolver());
        TransactionItemProcessListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        TransactionItemProcessListener.afterProcess(
                InboundTransaction.builder().filename("test").lineNumber(1).build(),
                null);

        Assert.assertEquals(1,
                FileUtils.listFiles(
                        resolver.getResources("classpath:/test-encrypt/**/testProcess")[0].getFile(),
                        new String[]{"csv"},false).size());

    }

    @SneakyThrows
    @Test
    public void onProcessError_OK() {

        File folder = tempFolder.newFolder("testProcess");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String executionDate = OffsetDateTime.now().format(fmt);

        TransactionItemProcessListener TransactionItemProcessListener = new TransactionItemProcessListener();
        TransactionItemProcessListener.setExecutionDate(executionDate);
        TransactionItemProcessListener.setResolver(new PathMatchingResourcePatternResolver());
        TransactionItemProcessListener.setErrorTransactionsLogsPath("file:/"+folder.getAbsolutePath());
        TransactionItemProcessListener.onProcessError(
                InboundTransaction.builder().filename("test").lineNumber(1).build(),
                new Exception());

        Assert.assertEquals(1,
                FileUtils.listFiles(
                        resolver.getResources("classpath:/test-encrypt/**/testProcess")[0].getFile(),
                        new String[]{"csv"},false).size());

    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}