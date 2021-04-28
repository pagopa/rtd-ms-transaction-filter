package it.gov.pagopa.rtd.transaction_filter.batch.step.processor;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import it.gov.pagopa.rtd.transaction_filter.service.BinStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.TokenPanStoreService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import javax.validation.*;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the ItemProcessor interface, used to process instances of InboundTransaction,
 * to be mapped into a normalized version defined as instances of Transaction
 */

@Slf4j
@Data
@RequiredArgsConstructor
public class InboundBinTokenPanItemProcessor implements ItemProcessor<InboundTokenPan, InboundTokenPan> {

    private final BinStoreService binStoreService;
    private final Boolean lastSection;
    private final Boolean binValidationEnabled;
    private List<String> exemptedCircuitType;

    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();


    /**
     * Validates the input {@link InboundTokenPan}, and filters the model, if the pan is not available
     * in the {@link BinStoreService}. Optionally applies the hashing function to the transaction PAN
     * @param inboundTokenPan
     *              instance of {@link InboundTokenPan} from the read phase of the step
     * @return instance of  {@link InboundTokenPan}
     * @throws ConstraintViolationException
     */
    @Override
    public InboundTokenPan process(InboundTokenPan inboundTokenPan) {

        Set<ConstraintViolation<InboundTokenPan>> constraintViolations =
                validator.validate(inboundTokenPan);
        if (constraintViolations.size() > 0) {
            throw new ConstraintViolationException(constraintViolations);
        }

        boolean hasBin = !binValidationEnabled ||
                (exemptedCircuitType.contains(inboundTokenPan.getCircuitType()) &&
                binStoreService.hasBin(inboundTokenPan.getTokenPan().substring(0,4)));

        if (hasBin) {
            return inboundTokenPan;
        } else {
            if (lastSection) {
                return null;
            }
            inboundTokenPan.setValid(false);
        }

        return inboundTokenPan;

    }

}
