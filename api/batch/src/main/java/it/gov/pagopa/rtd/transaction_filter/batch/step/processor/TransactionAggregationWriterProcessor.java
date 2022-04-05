package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import it.gov.pagopa.rtd.transaction_filter.batch.model.AdeTransactionsAggregate;
import it.gov.pagopa.rtd.transaction_filter.service.AggregationKey;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * TODO
 */
@Slf4j
@RequiredArgsConstructor
public class TransactionAggregationWriterProcessor implements ItemProcessor<AggregationKey, AdeTransactionsAggregate> {

    private final StoreService storeService;

    /**
     * TODO
     */
    @Override
    public AdeTransactionsAggregate process(AggregationKey key) {
        AdeTransactionsAggregate aggregate = new AdeTransactionsAggregate();
        aggregate.setAcquirerCode(key.getAcquirerCode());
        aggregate.setAcquirerId(key.getAcquirerId());
        aggregate.setTerminalId(key.getTerminalId());
        aggregate.setAccountingDate(key.getAccountingDate());
        aggregate.setFiscalCode(key.getFiscalCode());
        aggregate.setMerchantId(key.getMerchantId());
        aggregate.setOperationType(key.getOperationType());
        aggregate.setNumTrx(storeService.getAggregate(key).getNumTrx().intValue());
        aggregate.setTotalAmount(storeService.getAggregate(key).getTotalAmount().longValue());
        return aggregate;
    }
}
