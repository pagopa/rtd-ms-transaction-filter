package it.gov.pagopa.rtd.transaction_filter.batch.step.classifier;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.ItemWriter;

public class InboundTokenPanClassifierTest {

    public InboundTokenPanClassifierTest(){
        MockitoAnnotations.initMocks(this);
    }

    private InboundTokenPanClassifier inboundTokenPanClassifier;

    @Mock
    ItemWriter<InboundTokenPan> outputWriterMock;

    @Mock
    ItemWriter<InboundTokenPan> filterWriterMock;

    @Before
    public void setUp() {
        Mockito.reset(outputWriterMock, filterWriterMock);
        inboundTokenPanClassifier = new InboundTokenPanClassifier(outputWriterMock,filterWriterMock);
    }

    @Test
    public void test_classify_validToken() {
        ItemWriter returnWriter = inboundTokenPanClassifier.classify(getInboundTokenPan());
        Assert.assertEquals(outputWriterMock, returnWriter);
    }

    @Test
    public void test_classify_invalidToken() {
        InboundTokenPan inboundTokenPan = getInboundTokenPan();
        inboundTokenPan.setValid(false);
        ItemWriter returnWriter = inboundTokenPanClassifier.classify(inboundTokenPan);
        Assert.assertEquals(filterWriterMock, returnWriter);
    }


    public InboundTokenPan getInboundTokenPan() {
        return InboundTokenPan.builder()
                .tokenPan("00000")
                .circuitType("01")
                .par("0000")
                .valid(true)
                .filename("test.csv")
                .lineNumber(1)
                .build();
    }

}