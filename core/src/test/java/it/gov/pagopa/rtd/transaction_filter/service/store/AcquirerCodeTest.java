package it.gov.pagopa.rtd.transaction_filter.service.store;

import org.junit.Assert;
import org.junit.Test;

public class AcquirerCodeTest {

    @Test
    public void getCodeShouldReturnExpectedValue() {
        String code = "CODE123";
        AcquirerCode acquirerCode = new AcquirerCode(code);
        Assert.assertEquals(acquirerCode.getCode(), code);
    }
}