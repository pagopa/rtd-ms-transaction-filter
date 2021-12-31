package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.batch.item.ItemProcessor;

import javax.validation.*;
import java.util.Set;

/**
 * Implementation of the ItemProcessor interface, used to process instances of InboundTransaction,
 * to be mapped into a normalized version defined as instances of Transaction
 */

@Slf4j
@RequiredArgsConstructor
public class InboundTransactionItemAdeProcessor implements ItemProcessor<InboundTransaction, InboundTransaction> {

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();


    /**
     * TODO
     */
    @Override
    public InboundTransaction process(InboundTransaction inboundTransaction) {

        Set<ConstraintViolation<InboundTransaction>> constraintViolations =
                validator.validate(inboundTransaction);
        if (constraintViolations.size() > 0) {
            throw new ConstraintViolationException(constraintViolations);
        }

        return inboundTransaction;
    }

}
