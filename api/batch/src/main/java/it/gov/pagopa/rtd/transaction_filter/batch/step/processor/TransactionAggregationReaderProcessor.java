package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.AggregationKey;
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
 * TODO
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
        if (constraintViolations.size() > 0) {
            throw new ConstraintViolationException(constraintViolations);
        }

        AggregationKey key = new AggregationKey();
        key.setAcquirerCode(inboundTransaction.getAcquirerCode());
        key.setAcquirerId(inboundTransaction.getAcquirerId());
        key.setMerchantId(inboundTransaction.getMerchantId());
        key.setTerminalId(inboundTransaction.getTerminalId());
        key.setFiscalCode(inboundTransaction.getFiscalCode());
        key.setOperationType(inboundTransaction.getOperationType());
        key.setAccountingDate(inboundTransaction.getTrxDate().substring(0, 10));
        storeService.storeAggregate(key, inboundTransaction.getAmount());

        // Return null since we'll bound to a dummy writer
        return null;
    }
}
