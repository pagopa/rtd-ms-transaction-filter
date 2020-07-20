package it.gov.pagopa.rtd.transaction_filter.batch.encryption.exception;

/**
 * Custom {@link Throwable} used to define errors in the decrypt phase of the reader
 */
public class PGPDecryptException extends Throwable {

    public PGPDecryptException() {
        super();
    }

    public PGPDecryptException(String message, Throwable cause) {
        super(message, cause);
    }

}
