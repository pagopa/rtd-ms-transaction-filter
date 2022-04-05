package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import it.gov.pagopa.rtd.transaction_filter.batch.model.AdeTransactionsAggregate;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.Resource;

/**
 * TODO
 */

@Slf4j
@RequiredArgsConstructor
public class PGPFlatFileAggregateWriter extends FlatFileItemWriter<AdeTransactionsAggregate> {

    private final String publicKey;
    private final boolean applyEncrypt;
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
        if (applyEncrypt) {
            ByteArrayInputStream publicKeyIS = null;
            FileOutputStream outputFOS = null;
            try {
                publicKeyIS = new ByteArrayInputStream(publicKey.getBytes());
                outputFOS = new FileOutputStream(resource.getFile().getAbsolutePath().concat(".pgp"));
                PGPPublicKey pgpPublicKey = EncryptUtil.readPublicKey(publicKeyIS);
                String fingerprint = new String(Hex.encode(pgpPublicKey.getFingerprint())).toUpperCase(Locale.ROOT);
                log.info("Encrypting file " + resource.getFilename() + " with PGP key " + fingerprint);
                EncryptUtil.encryptFile(outputFOS,
                        this.resource.getFile().getAbsolutePath(),
                        pgpPublicKey,
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
