package it.gov.pagopa.rtd.transaction_filter.service.store;

import org.junit.Assert;
import org.junit.Test;

public class AcquirerIdTest {

    @Test
    public void bla() {
        String id = "ID123";
        AcquirerId acquirerId = new AcquirerId(id);
        Assert.assertTrue(acquirerId.getId().equals(id));
    }
}