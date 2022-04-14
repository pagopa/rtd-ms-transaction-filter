package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.util.Locale;


@Slf4j
@RequiredArgsConstructor
public class PGPFlatFileItemWriter<T> extends FlatFileItemWriter<T> {

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
            try (ByteArrayInputStream publicKeyIS = new ByteArrayInputStream(publicKey.getBytes()); FileOutputStream outputFOS = new FileOutputStream(resource.getFile().getAbsolutePath().concat(".pgp"))) {
                PGPPublicKey pgpPublicKey = EncryptUtil.readPublicKey(publicKeyIS);
                String fingerprint = new String(Hex.encode(pgpPublicKey.getFingerprint())).toUpperCase(Locale.ROOT);
                log.info("Encrypting file " + resource.getFilename() + " with PGP key " + fingerprint);
                EncryptUtil.encryptFile(outputFOS,
                        this.resource.getFile().getAbsolutePath(),
                        pgpPublicKey,
                        false, true);
            }
        }
    }
}
