package it.gov.pagopa.rtd.transaction_filter.batch.step.tasklet;

import it.gov.pagopa.rtd.transaction_filter.batch.encryption.EncryptUtil;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;

/**
 * Implementation of the {@link Tasklet}, implements the file encryption routine
 */

@Slf4j
@Data
@RequiredArgsConstructor
public class PGPEncrypterTasklet implements Tasklet {

  private boolean taskletEnabled = false;
  private String publicKey;
  private Resource fileToEncrypt;

  /**
   * Generates a .pgp file from a file using a public key for the encryption
   *
   * @return task exit status
   */
  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
      throws IOException, PGPException {

    if (taskletEnabled) {
      try (ByteArrayInputStream publicKeyIS = new ByteArrayInputStream(publicKey.getBytes());
          FileOutputStream outputFOS = new FileOutputStream(
              fileToEncrypt.getFile().getAbsolutePath().concat(".pgp"))) {
        PGPPublicKey pgpPublicKey = EncryptUtil.readPublicKey(publicKeyIS);
        String fingerprint = new String(Hex.encode(pgpPublicKey.getFingerprint())).toUpperCase(
            Locale.ROOT);
        log.info("Encrypting file " + fileToEncrypt.getFilename() + " with PGP key " + fingerprint);
        EncryptUtil.encryptFile(outputFOS,
            fileToEncrypt.getFile().getAbsolutePath(),
            pgpPublicKey,
            false, true);
      }
    }

    return RepeatStatus.FINISHED;
  }
}
