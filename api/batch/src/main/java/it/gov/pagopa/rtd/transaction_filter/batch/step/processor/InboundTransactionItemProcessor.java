package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
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
public class InboundTransactionItemProcessor implements ItemProcessor<InboundTransaction, InboundTransaction> {

    private final StoreService storeService;
    private final boolean applyHashing;

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();


    /**
     * Validates the input {@link InboundTransaction}, and filters the model, if the pan is not available
     * in the {@link StoreService}. Optionally applies the hashing function to the transaction PAN
     *
     * @param inboundTransaction instance of {@link InboundTransaction} from the read phase of the step
     * @return instance of  {@link InboundTransaction}
     * @throws ConstraintViolationException
     */
    @Override
    public InboundTransaction process(InboundTransaction inboundTransaction) {

        Set<ConstraintViolation<InboundTransaction>> constraintViolations =
                validator.validate(inboundTransaction);
        if (!constraintViolations.isEmpty()) {
            constraintViolations.forEach(violation ->
                log.warn("Validation failed on field: {}, value: {}, validation: {}, reason: {}",
                    violation.getPropertyPath(), violation.getInvalidValue(),
                    violation.getMessageTemplate(), violation.getMessage()));
            throw new ConstraintViolationException(constraintViolations);
        }

        String hpan = applyHashing ?
                DigestUtils.sha256Hex(inboundTransaction.getPan() + storeService.getSalt()) :
                inboundTransaction.getPan();

        if (storeService.hasHpan(hpan)) {
            InboundTransaction resultTransaction =
                    InboundTransaction
                            .builder()
                            .mcc(inboundTransaction.getMcc())
                            .lineNumber(inboundTransaction.getLineNumber())
                            .filename(inboundTransaction.getFilename())
                            .bin(inboundTransaction.getBin())
                            .terminalId(inboundTransaction.getTerminalId())
                            .merchantId(inboundTransaction.getMerchantId())
                            .acquirerId(inboundTransaction.getAcquirerId())
                            .amountCurrency(inboundTransaction.getAmountCurrency())
                            .correlationId(inboundTransaction.getCorrelationId())
                            .idTrxIssuer(inboundTransaction.getIdTrxIssuer())
                            .idTrxAcquirer(inboundTransaction.getIdTrxAcquirer())
                            .pan(inboundTransaction.getPan())
                            .circuitType(inboundTransaction.getCircuitType())
                            .operationType(inboundTransaction.getOperationType())
                            .senderCode(inboundTransaction.getSenderCode())
                            .amount(inboundTransaction.getAmount())
                            .trxDate(inboundTransaction.getTrxDate())
                            .fiscalCode(inboundTransaction.getFiscalCode())
                            .vat(inboundTransaction.getVat())
                            .posType(inboundTransaction.getPosType())
                            .par(inboundTransaction.getPar())
                            .build();

            resultTransaction.setPan(applyHashing ?
                    hpan : DigestUtils.sha256Hex(
                    resultTransaction.getPan() + storeService.getSalt()));

            return resultTransaction;
        } else {
            return null;
        }

    }

}
