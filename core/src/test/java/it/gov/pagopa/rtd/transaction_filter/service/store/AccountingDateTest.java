package it.gov.pagopa.rtd.transaction_filter.service.store;

import org.junit.Assert;
import org.junit.Test;

public class AccountingDateTest {

    @Test
    public void bla() {
        String date = "12-04-2022";
        AccountingDate accountingDate = new AccountingDate(date);
        Assert.assertTrue(accountingDate.getDate().equals(date));
    }
}