package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.store.AggregationKey;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * ItemProcessor responsible for in-memory aggregation of transaction data.
 */
@Slf4j
@RequiredArgsConstructor
public class TransactionAggregationReaderProcessor implements ItemProcessor<InboundTransaction, InboundTransaction> {

    private final StoreService storeService;

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    /**
     * Process inbound transactions and aggregates on the fly leveraging
     * the StoreService data structure.
     */
    @Override
    public InboundTransaction process(InboundTransaction inboundTransaction) {

        Set<ConstraintViolation<InboundTransaction>> constraintViolations =
                validator.validate(inboundTransaction);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }

        AggregationKey key = new AggregationKey();
        key.setAcquirerCode(inboundTransaction.getAcquirerCode());
        key.setAcquirerId(inboundTransaction.getAcquirerId());
        key.setMerchantId(inboundTransaction.getMerchantId());
        key.setTerminalId(inboundTransaction.getTerminalId());
        key.setFiscalCode(inboundTransaction.getFiscalCode());
        if (inboundTransaction.getOperationType().equals("00")) {
            key.setOperationType((byte)0);
        } else {
            key.setOperationType((byte)1);
        }
        key.setAccountingDate(inboundTransaction.getTrxDate().substring(0, 10));
        storeService.storeAggregate(key, inboundTransaction.getAmount(), inboundTransaction.getAmountCurrency(), inboundTransaction.getVat(), inboundTransaction.getPosType());

        // Return null since we'll bound to a dummy writer
        return null;
    }
}
