package it.gov.pagopa.rtd.transaction_filter.batch.step.classifier;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import org.springframework.batch.item.ItemWriter;
import org.springframework.classify.Classifier;

public class InboundTokenPanClassifier implements Classifier<InboundTokenPan, ItemWriter<? super InboundTokenPan>> {

    private static final long serialVersionUID = 1L;

    private final ItemWriter<InboundTokenPan> outputWriter;
    private final ItemWriter<InboundTokenPan> filterWriter;

    public InboundTokenPanClassifier(ItemWriter<InboundTokenPan> outputWriter,
                                     ItemWriter<InboundTokenPan> filterWriter) {
        this.outputWriter = outputWriter;
        this.filterWriter = filterWriter;
    }

    @Override
    public ItemWriter<? super InboundTokenPan> classify(InboundTokenPan inboundTokenPan) {
        return inboundTokenPan.getValid() ? outputWriter : filterWriter;
    }
}