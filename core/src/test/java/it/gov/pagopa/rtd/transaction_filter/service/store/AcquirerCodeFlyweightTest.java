package it.gov.pagopa.rtd.transaction_filter.service.store;

import javax.annotation.concurrent.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
@NotThreadSafe
public class AcquirerCodeFlyweightTest {

    @Test
    public void getCacheSizeShouldReturnExpectedValueWhenEmpty() {
        AcquirerCodeFlyweight acquirerCodeFlyweight = new AcquirerCodeFlyweight();
        Assert.assertEquals(0, acquirerCodeFlyweight.cacheSize());
    }

    @Test
    public void getCacheSizeShouldReturnExpectedValueWhenEntriesAdded() {
        AcquirerCodeFlyweight acquirerCodeFlyweight = new AcquirerCodeFlyweight();
        acquirerCodeFlyweight.createAcquirerCode("11111");
        acquirerCodeFlyweight.createAcquirerCode("22222");
        acquirerCodeFlyweight.createAcquirerCode("33333");
        Assert.assertEquals(3, acquirerCodeFlyweight.cacheSize());
    }
}