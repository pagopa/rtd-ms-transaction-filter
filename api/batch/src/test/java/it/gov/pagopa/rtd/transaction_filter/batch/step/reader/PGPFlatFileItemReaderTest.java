package it.gov.pagopa.rtd.transaction_filter.batch.step.reader;

import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.batch.encryption.exception.PGPDecryptException;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.core.io.UrlResource;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.io.FileOutputStream;

public class PGPFlatFileItemReaderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(
            new File(getClass().getResource("/test-encrypt").getFile()));


    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @SneakyThrows
    @Test
    public void testReader_Ok() {

        File panPgp = tempFolder.newFile("pan.pgp");

        FileOutputStream panFOS = new FileOutputStream(panPgp);

        EncryptUtil.encryptFile(panFOS,
                this.getClass().getResource("/test-encrypt/pan").getFile() + "/pan.csv",
                EncryptUtil.readPublicKey(
                        this.getClass().getResourceAsStream("/test-encrypt/publicKey.asc")),
                false,false);
        PGPFlatFileItemReader flatFileItemReader = new PGPFlatFileItemReader(
                "file:/"+ this.getClass().getResource("/test-encrypt").getFile() +
                        "/secretKey.asc", "test", true);
        panFOS.close();
        flatFileItemReader.setResource(new UrlResource(tempFolder.getRoot().toURI() + "pan.pgp"));
        flatFileItemReader.setLineMapper(new PassThroughLineMapper());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemReader.update(executionContext);
        flatFileItemReader.open(executionContext);
        Assert.assertNotNull(flatFileItemReader.read());
        Assert.assertNotNull(flatFileItemReader.read());
        Assert.assertNotNull(flatFileItemReader.read());
        flatFileItemReader.update(executionContext);
        Assert.assertEquals(3, executionContext
                .getInt(ClassUtils.getShortName(FlatFileItemReader.class) + ".read.count"));
    }

    @SneakyThrows
    @Test
    public void testReader_WrongKey() {

        File panPgp = tempFolder.newFile("pan.pgp");

        FileOutputStream textTrxPgpFOS = new FileOutputStream(panPgp);

        EncryptUtil.encryptFile(textTrxPgpFOS,
                this.getClass().getResource("/test-encrypt/pan").getFile() + "/pan.csv",
                EncryptUtil.readPublicKey(
                        this.getClass().getResourceAsStream("/test-encrypt/otherPublicKey.asc")),
                false,false);

        textTrxPgpFOS.close();

        PGPFlatFileItemReader flatFileItemReader = new PGPFlatFileItemReader(
                "file:/"+this.getClass().getResource("/test-encrypt").getFile() +
                        "/secretKey.asc", "test", true);
        flatFileItemReader.setResource(new UrlResource(tempFolder.getRoot().toURI() + "pan.pgp"));
        flatFileItemReader.setLineMapper(new PassThroughLineMapper());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemReader.update(executionContext);
        exceptionRule.expect(PGPDecryptException.class);
        flatFileItemReader.open(executionContext);
        Assert.assertEquals(0, executionContext
                .getInt(ClassUtils.getShortName(FlatFileItemReader.class) + ".read.count"));
    }

    @SneakyThrows
    @Test
    public void testReader_NoEncrypt() {

        PGPFlatFileItemReader flatFileItemReader = new PGPFlatFileItemReader(
                "file:/"+this.getClass().getResource("/test-encrypt").getFile() +
                        "/secretKey.asc", "test", false);
        flatFileItemReader.setResource(new UrlResource("file:"+
                this.getClass().getResource("/test-encrypt/pan/pan.csv").getFile()));
        flatFileItemReader.setLineMapper(new PassThroughLineMapper());
        ExecutionContext executionContext = MetaDataInstanceFactory.createStepExecution().getExecutionContext();
        flatFileItemReader.update(executionContext);
        flatFileItemReader.open(executionContext);
        Assert.assertNotNull(flatFileItemReader.read());
        Assert.assertNotNull(flatFileItemReader.read());
        Assert.assertNotNull(flatFileItemReader.read());
        flatFileItemReader.update(executionContext);
        Assert.assertEquals(3, executionContext
                .getInt(ClassUtils.getShortName(FlatFileItemReader.class) + ".read.count"));
    }

    @After
    public void tearDown() {
        tempFolder.delete();
    }

}