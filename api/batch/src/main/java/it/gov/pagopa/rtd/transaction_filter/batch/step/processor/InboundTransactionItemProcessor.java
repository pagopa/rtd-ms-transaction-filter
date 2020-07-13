package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;

import javax.validation.*;
import java.util.Set;

/**
 * Implementation of the ItemProcessor interface, used to process instances of InboundTransaction,
 * to be mapped into a normalized version defined as instances of Transaction
 */

@Slf4j
@RequiredArgsConstructor
public class InboundTransactionItemProcessor implements ItemProcessor<InboundTransaction, InboundTransaction> {

    private final HpanStoreService hpanStoreService;
    private final Boolean applyHashing;
    private final Boolean saveHashing;
    private String salt = "";

    /**
     * Validates the input {@link InboundTransaction}, and maps it to an instance of Transaction
     * @param inboundTransaction
     *              instance of {@link InboundTransaction} from the read phase of the step
     * @return instance of  {@link InboundTransaction}
     * @throws ConstraintViolationException
     */
    @Override
    public InboundTransaction process(InboundTransaction inboundTransaction) {

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        Set<ConstraintViolation<InboundTransaction>> constraintViolations = validator.validate(inboundTransaction);
        if (constraintViolations.size() > 0) {
            throw new ConstraintViolationException(constraintViolations);
        }

        String hpan = applyHashing ?
                DigestUtils.sha256Hex(inboundTransaction.getPan()+salt) :
                inboundTransaction.getPan();

        if (hpanStoreService.hasHpan(hpan)) {
            if (saveHashing) {
                inboundTransaction.setPan(applyHashing ?
                        hpan : DigestUtils.sha256Hex(inboundTransaction.getPan()+salt));
            }
            return inboundTransaction;
        }
        return null;
    }

    @BeforeStep
    public void recoverSalt(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        this.salt = jobContext.containsKey("salt") ? String.valueOf(jobContext.get("salt")) : "";
    }

}
