package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;

/**
 * Implementation of {@link FlatFileItemWriter}, to be used for writing the output transaction records, potentially
 * encrypting the output file in the pgp phase
 */

@Slf4j
@RequiredArgsConstructor
public class PGPFlatFileItemWriter extends FlatFileItemWriter<InboundTransaction> {

    private final String publicKey;
    private final Boolean applyEncrypt;
    private Resource resource;
    private Boolean isClosed = false;

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
        super.setResource(resource);
    }

    @SneakyThrows
    @Override
    public void close() {
        super.close();
        if (applyEncrypt && !isClosed) {
            ByteArrayInputStream publicKeyIS = null;
            FileOutputStream outputFOS = null;
            try {
                publicKeyIS = new ByteArrayInputStream(publicKey.getBytes());
                outputFOS = new FileOutputStream(resource.getFile().getAbsolutePath().concat(".pgp"));
                PGPPublicKey publicKey = EncryptUtil.readPublicKey(publicKeyIS);
                String fingerprint = new String(Hex.encode(publicKey.getFingerprint())).toUpperCase(Locale.ROOT);
                log.info("Encrypting file " + resource.getFilename() + " with PGP key " + fingerprint);
                EncryptUtil.encryptFile(outputFOS,
                        this.resource.getFile().getAbsolutePath(),
                        publicKey,
                        false, true);
                isClosed = true;
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
