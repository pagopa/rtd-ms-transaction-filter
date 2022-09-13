package it.gov.pagopa.rtd.transaction_filter.batch.step.reader;

import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.batch.encryption.exception.PGPDecryptException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * Extension of {@link FlatFileItemReader}, in which a decryption phase is
 * added to extract the csv content from the .pgp files
 */

@RequiredArgsConstructor
@Slf4j
public class PGPFlatFileItemReader extends FlatFileItemReader<String> {

    private final String secretFilePath;
    private final String passphrase;
    private final Boolean applyDecrypt;

    private Resource resource;

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
        super.setResource(resource);
    }

    /**
     * Override of {@link FlatFileItemReader#doOpen},introduces a
     * decrypt pass before calling on the parent implementation
     */
    @SneakyThrows
    @Override
    protected void doOpen() {

        Assert.notNull(this.resource, "Input resource must be set");

        if (Boolean.TRUE.equals(applyDecrypt)) {

            File fileToProcess = resource.getFile();
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource secretKeyResource = resolver.getResource(secretFilePath);
            try (FileInputStream fileToProcessIS = new FileInputStream(fileToProcess);
                FileInputStream secretFilePathIS = new FileInputStream(secretKeyResource.getFile())) {
                byte[] decryptFileData = EncryptUtil.decryptFile(
                        fileToProcessIS,
                        secretFilePathIS,
                        passphrase.toCharArray()
                );
                super.setResource(new InputStreamResource(
                        new ByteArrayInputStream(decryptFileData)));
            } catch (Exception e) {
                log.error(e.getMessage(),e);
                throw new PGPDecryptException(e.getMessage(),e);
            }

        } else {
            super.setResource(resource);
        }

        super.doOpen();
        log.debug("Reader Opened");

    }

}