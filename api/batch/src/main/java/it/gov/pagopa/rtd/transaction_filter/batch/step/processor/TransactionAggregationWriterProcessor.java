package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import it.gov.pagopa.rtd.transaction_filter.batch.model.AdeTransactionsAggregate;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationKey;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * ItemProcessor responsible for creating aggregates models suited for CSV
 * writing from the previously in-memory aggregated data.
 */
@Slf4j
@RequiredArgsConstructor
public class TransactionAggregationWriterProcessor implements ItemProcessor<AggregationKey, AdeTransactionsAggregate> {

    private final static String DIRTY_POS_TYPE = "99";
    private final StoreService storeService;
    private final String transmissionDate;

    /**
     * Given an aggregation clause (i.e. key) build a data model of the aggregation
     * suitable for CSV encoding by the ItemWriter.
     */
    @Override
    public AdeTransactionsAggregate process(AggregationKey key) {
        AdeTransactionsAggregate aggregate = new AdeTransactionsAggregate();
        aggregate.setAcquirerCode(key.getAcquirerCode().getCode());
        if (key.getOperationType() == 0) {
            aggregate.setOperationType("00");
        } else {
            aggregate.setOperationType("01");
        }
        aggregate.setTransmissionDate(transmissionDate);
        aggregate.setAccountingDate(key.getAccountingDate().getDate());
        aggregate.setNumTrx(storeService.getAggregate(key).getNumTrx());
        aggregate.setTotalAmount((long) storeService.getAggregate(key).getTotalAmount());
        aggregate.setCurrency(storeService.getAggregate(key).getCurrency().getIsoCode());
        aggregate.setAcquirerId(key.getAcquirerId().getId());
        aggregate.setMerchantId(key.getMerchantId());
        aggregate.setTerminalId(key.getTerminalId());
        aggregate.setFiscalCode(key.getFiscalCode());
        aggregate.setVat(storeService.getAggregate(key).getVat());
        if (storeService.getAggregate(key).getPosType() == 0) {
            aggregate.setPosType("00");
        } else if (storeService.getAggregate(key).getPosType() == 1) {
            aggregate.setPosType("01");
        } else {
            aggregate.setPosType(DIRTY_POS_TYPE);
        }
        return aggregate;
    }
}
