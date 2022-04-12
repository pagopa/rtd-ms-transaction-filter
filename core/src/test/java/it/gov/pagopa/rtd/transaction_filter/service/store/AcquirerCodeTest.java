package it.gov.pagopa.rtd.transaction_filter.service.store;

import org.junit.Assert;
import org.junit.Test;

public class AcquirerCodeTest {

    @Test
    public void bla() {
        String code = "CODE123";
        AcquirerCode acquirerCode = new AcquirerCode(code);
        Assert.assertTrue(acquirerCode.getCode().equals(code));
    }
}