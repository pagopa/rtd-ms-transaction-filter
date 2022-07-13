package it.gov.pagopa.rtd.transaction_filter.service.store;

import javax.annotation.concurrent.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
@NotThreadSafe
public class SenderCodeFlyweightTest {

    @Test
    public void getCacheSizeShouldReturnExpectedValueWhenEmpty() {
        SenderCodeFlyweight senderCodeFlyweight = new SenderCodeFlyweight();
        Assert.assertEquals(0, senderCodeFlyweight.cacheSize());
    }

    @Test
    public void getCacheSizeShouldReturnExpectedValueWhenEntriesAdded() {
        SenderCodeFlyweight senderCodeFlyweight = new SenderCodeFlyweight();
        senderCodeFlyweight.createSenderCode("11111");
        senderCodeFlyweight.createSenderCode("22222");
        senderCodeFlyweight.createSenderCode("33333");
        Assert.assertEquals(3, senderCodeFlyweight.cacheSize());
    }
}