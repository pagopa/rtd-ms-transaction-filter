package it.gov.pagopa.rtd.transaction_filter.batch.encryption.exception;

/**
 * Custom {@link Throwable} used to define errors in the decrypt phase of the reader
 */
public class PGPDecryptThrowable extends Throwable {

    public PGPDecryptThrowable() {
        super();
    }

    public PGPDecryptThrowable(String message, Throwable cause) {
        super(message, cause);
    }

}
