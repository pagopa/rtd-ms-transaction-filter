package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Implementation of {@link FlatFileItemWriter}, to be used for writing the output transaction records, potentially
 * encrypting the output file in the pgp phase
 */

@RequiredArgsConstructor
public class PGPFlatFileItemWriter extends FlatFileItemWriter<InboundTransaction> {

    private final String publicKeyPath;
    private final Boolean applyEncrypt;
    private final Boolean lastSection;

    private Resource resource;

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
        super.setResource(resource);
    }

    @SneakyThrows
    @Override
    public void close() {
        super.close();
        if (applyEncrypt && lastSection) {
            FileInputStream publicKeyIS = null;
            FileOutputStream outputFOS = null;
            try {
                resource.getFilename();
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource publicKeyResource = resolver.getResource(publicKeyPath);
                publicKeyIS = new FileInputStream(publicKeyResource.getFile());
                outputFOS = new FileOutputStream(resource.getFile().getAbsolutePath().concat(".pgp"));
                EncryptUtil.encryptFile(outputFOS,
                        this.resource.getFile().getAbsolutePath(),
                        EncryptUtil.readPublicKey(publicKeyIS),
                        false, true);
            } finally {
                if (publicKeyIS != null) {
                    publicKeyIS.close();
                }
                if (outputFOS != null) {
                    outputFOS.close();
                }
            }
        }
    }



}
