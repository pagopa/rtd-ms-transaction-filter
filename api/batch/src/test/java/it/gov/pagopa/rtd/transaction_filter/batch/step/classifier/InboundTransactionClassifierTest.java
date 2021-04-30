package it.gov.pagopa.rtd.transaction_filter.batch.step.classifier;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.ItemWriter;

public class InboundTransactionClassifierTest {

    public InboundTransactionClassifierTest(){
        MockitoAnnotations.initMocks(this);
    }

    private InboundTransactionClassifier inboundTransactionClassifier;

    @Mock
    ItemWriter<InboundTransaction> outputWriterMock;

    @Mock
    ItemWriter<InboundTransaction> filterWriterMock;

    @Before
    public void setUp() {
        Mockito.reset(outputWriterMock, filterWriterMock);
        inboundTransactionClassifier = new InboundTransactionClassifier(outputWriterMock,filterWriterMock);
    }

    @Test
    public void test_classify_validToken() {
        ItemWriter returnWriter = inboundTransactionClassifier.classify(getInboundTransaction());
        Assert.assertEquals(outputWriterMock, returnWriter);
    }

    @Test
    public void test_classify_invalidToken() {
        InboundTransaction inboundTransaction = getInboundTransaction();
        inboundTransaction.setValid(false);
        ItemWriter returnWriter = inboundTransactionClassifier.classify(inboundTransaction);
        Assert.assertEquals(filterWriterMock, returnWriter);
    }


    protected InboundTransaction getInboundTransaction() {
        return InboundTransaction.builder()
                .idTrxAcquirer("1")
                .acquirerCode("001")
                .trxDate("2020-04-09T16:22:45.304Z")
                .amount(1000L)
                .operationType("00")
                .pan("pan")
                .merchantId("0")
                .circuitType("00")
                .mcc("813")
                .idTrxIssuer("0")
                .amountCurrency("833")
                .correlationId("1")
                .acquirerId("0")
                .terminalId("0")
                .bin("000001")
                .par("00001")
                .valid(true)
                .build();
    }

}