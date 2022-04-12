package it.gov.pagopa.rtd.transaction_filter.service.store;

import org.junit.Assert;
import org.junit.Test;

public class CurrencyTest {

    @Test
    public void bla() {
        String isoCode = "ID123";
        Currency currency = new Currency(isoCode);
        Assert.assertEquals(currency.getIsoCode(), isoCode);
    }
}