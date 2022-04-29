package it.gov.pagopa.rtd.transaction_filter.service.store;

import org.junit.Assert;
import org.junit.Test;

public class AggregationDataTest {

    private final static String VAT = "VAT123";
    private final static String NULL_VAT = null;

    private final static String POS_TYPE_PHYSICAL = "00";
    private final static String POS_TYPE_ECOMMERCE = "01";

    @Test
    public void updateVatShouldHandleCleanData() {
        AggregationData data = new AggregationData();
        data.updateVatOrMarkAsDirty(VAT);
        data.updateVatOrMarkAsDirty(VAT);
        data.updateVatOrMarkAsDirty(VAT);
        Assert.assertEquals(VAT, data.getVat());
    }

    @Test
    public void updateVatShouldHandleCleanDataBis() {
        AggregationData data = new AggregationData();
        data.updateVatOrMarkAsDirty(NULL_VAT);
        data.updateVatOrMarkAsDirty(NULL_VAT);
        data.updateVatOrMarkAsDirty(NULL_VAT);
        Assert.assertEquals(NULL_VAT, data.getVat());
    }

    @Test
    public void updateVatShouldHandleDirtyData() {
        AggregationData data = new AggregationData();
        data.updateVatOrMarkAsDirty(VAT);
        data.updateVatOrMarkAsDirty("VAT456");
        data.updateVatOrMarkAsDirty(VAT);
        Assert.assertEquals(AggregationData.DIRTY_VAT, data.getVat());
    }

    @Test
    public void updateVatShouldHandleDirtyDataBis() {
        AggregationData data = new AggregationData();
        data.updateVatOrMarkAsDirty(VAT);
        data.updateVatOrMarkAsDirty(NULL_VAT);
        data.updateVatOrMarkAsDirty(VAT);
        Assert.assertEquals(AggregationData.DIRTY_VAT, data.getVat());
    }

    @Test
    public void updateVatShouldHandleDirtyDataTer() {
        AggregationData data = new AggregationData();
        data.updateVatOrMarkAsDirty(NULL_VAT);
        data.updateVatOrMarkAsDirty(VAT);
        data.updateVatOrMarkAsDirty(NULL_VAT);
        Assert.assertEquals(AggregationData.DIRTY_VAT, data.getVat());
    }

    @Test
    public void updatePosTypeShouldHandleCleanData() {
        AggregationData data = new AggregationData();
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_PHYSICAL);
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_PHYSICAL);
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_PHYSICAL);
        Assert.assertEquals((byte) 0, data.getPosType());
    }

    @Test
    public void updatePosTypeShouldHandleCleanDataBis() {
        AggregationData data = new AggregationData();
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_ECOMMERCE);
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_ECOMMERCE);
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_ECOMMERCE);
        Assert.assertEquals((byte) 1, data.getPosType());
    }

    @Test
    public void updatePosTypeShouldHandleDirtyData() {
        AggregationData data = new AggregationData();
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_ECOMMERCE);
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_PHYSICAL);
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_PHYSICAL);
        Assert.assertEquals(AggregationData.DIRTY_POS_TYPE, data.getPosType());
    }

    @Test
    public void updatePosTypeShouldHandleDirtyDataBis() {
        AggregationData data = new AggregationData();
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_PHYSICAL);
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_ECOMMERCE);
        data.updatePosTypeOrMarkAsDirty(POS_TYPE_ECOMMERCE);
        Assert.assertEquals(AggregationData.DIRTY_POS_TYPE, data.getPosType());
    }
}