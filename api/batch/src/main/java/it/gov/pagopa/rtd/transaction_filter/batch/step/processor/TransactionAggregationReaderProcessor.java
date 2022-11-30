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
        key.setSenderCode(storeService.flyweightSenderCode(inboundTransaction.getSenderCode()));
        key.setAcquirerId(storeService.flyweightAcquirerIdToFiscalCode(inboundTransaction.getAcquirerId()));
        key.setMerchantId(inboundTransaction.getMerchantId());
        key.setTerminalId(inboundTransaction.getTerminalId());
        key.setFiscalCode(inboundTransaction.getFiscalCode());
        if (inboundTransaction.getOperationType().equals("00")) {
            key.setOperationType((byte) 0);
        } else {
            key.setOperationType((byte) 1);
        }
        key.setAccountingDate(storeService.flyweightAccountingDate(
            inboundTransaction.getTrxDate().substring(0, 10)));
        boolean dirtyDataFound = storeService.storeAggregate(key, inboundTransaction.getAmount(), inboundTransaction.getVat(),
            inboundTransaction.getPosType());

        if (dirtyDataFound) {
            log.warn("Dirty data found on either VAT or POS type fields at line {}", inboundTransaction.getLineNumber());
        }

        // Return the input transaction since it's needed by the listener to log validation errors
        // The writer is dummy and do not require any value
        return inboundTransaction;
    }
}
