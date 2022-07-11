package it.gov.pagopa.rtd.transaction_filter.service.store;

import org.junit.Assert;
import org.junit.Test;

public class SenderCodeTest {

    @Test
    public void getCodeShouldReturnExpectedValue() {
        String code = "CODE123";
        SenderCode senderCode = new SenderCode(code);
        Assert.assertEquals(senderCode.getCode(), code);
    }
}