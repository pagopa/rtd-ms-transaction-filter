package it.gov.pagopa.rtd.transaction_filter.batch.step.classifier;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import org.springframework.batch.item.ItemWriter;
import org.springframework.classify.Classifier;

public class InboundTransactionClassifier implements Classifier<InboundTransaction, ItemWriter<? super InboundTransaction>> {

    private static final long serialVersionUID = 1L;

    private final ItemWriter<InboundTransaction> outputWriter;
    private final ItemWriter<InboundTransaction> filterWriter;

    public InboundTransactionClassifier(ItemWriter<InboundTransaction> outputWriter,
                                        ItemWriter<InboundTransaction> filterWriter) {
        this.outputWriter = outputWriter;
        this.filterWriter = filterWriter;
    }

    @Override
    public ItemWriter<? super InboundTransaction> classify(InboundTransaction transaction) {
        return transaction.getValid() ? outputWriter : filterWriter;
    }
}