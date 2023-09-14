package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.batch.utils.TransactionMaskPolicy;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import java.util.Optional;
import jakarta.validation.ConstraintViolationException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Implementation of {@link ItemProcessListener}, to be used to log and/or store records
 * filtered or that have produced an error during a record processing phase
 */
@Slf4j
@Data
@RequiredArgsConstructor
public class TransactionItemProcessListener implements ItemProcessListener<InboundTransaction, InboundTransaction> {

    private String errorTransactionsLogsPath;
    private String executionDate;
    private Boolean enableOnErrorLogging;
    private Boolean enableOnErrorFileLogging;
    private Boolean enableAfterProcessLogging;
    private Boolean enableAfterProcessFileLogging;
    private Long loggingFrequency;
    private String prefix;
    private TransactionWriterService transactionWriterService;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final TransactionMaskPolicy maskPolicy;

    @Override
    public void beforeProcess(@NonNull InboundTransaction inboundTransaction) {
        // do nothing
    }

    public void afterProcess(@NonNull InboundTransaction item, @Nullable InboundTransaction result) {

        if (Boolean.TRUE.equals(enableAfterProcessLogging)) {
            if (result == null) {
                logFilteredItem(item);
            } else {
                logProcessedItem(item);
            }
        }

        if (Boolean.TRUE.equals(enableAfterProcessFileLogging) && result == null) {
            maskPolicy.apply(item);
            logItemIntoFilteredRecordsFile(item);
        }

    }

    private void logFilteredItem(InboundTransaction item) {
        if (loggingFrequency > 1 && item.getLineNumber() % loggingFrequency == 0) {
            log.info("Filtered transaction record on filename: {},line: {}",
                item.getFilename(),
                item.getLineNumber());
        } else if (loggingFrequency == 1) {
            log.debug("Filtered transaction record on filename: {},line: {}",
                item.getFilename(),
                item.getLineNumber());
        }
    }

    private void logProcessedItem(InboundTransaction item) {
        if (loggingFrequency > 1 && item.getLineNumber() % loggingFrequency == 0) {
            log.info("Processed {} lines on file: {}", item.getLineNumber(), item.getFilename());
        } else if (loggingFrequency == 1) {
            log.debug("Processed transaction record on filename: {}, line: {}",
                item.getFilename(), item.getLineNumber());
        }
    }

    private void logItemIntoFilteredRecordsFile(InboundTransaction item) {
        try {
            String file = item.getFilename().replace("\\", "/");
            String[] fileArr = file.split("/");
            transactionWriterService.write(resolver.getResource(errorTransactionsLogsPath)
                .getFile().getAbsolutePath()
                .concat("/".concat(executionDate))
                + "_" + prefix + "_FilteredRecords_" + fileArr[fileArr.length-1], buildCsv(item));
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void onProcessError(InboundTransaction item, Exception throwable) {
        maskPolicy.apply(item);

        if (Boolean.TRUE.equals(transactionWriterService.hasErrorHpan(item.getFilename()
                .concat(String.valueOf(item.getLineNumber()))))) {
            return;
        }

        transactionWriterService.storeErrorPans(item.getFilename()
                .concat(String.valueOf(item.getLineNumber())));

        if (Boolean.TRUE.equals(enableOnErrorLogging)) {
            logValidationErrors(item, throwable);
        }

        if (Boolean.TRUE.equals(enableOnErrorFileLogging)) {
            logSkippedLinesOnFile(item);
        }

    }

    private void logValidationErrors(InboundTransaction item, Exception throwable) {
        if (throwable instanceof ConstraintViolationException) {
            ((ConstraintViolationException) throwable).getConstraintViolations()
                .forEach(violation -> log.error("Error during record validation at line: {}, on field: {}, value: {}, validation: {}, reason: {}",
                    item.getLineNumber(), violation.getPropertyPath(), violation.getInvalidValue(),
                    violation.getMessageTemplate(), violation.getMessage()));

        } else {
            log.error("Error during transaction processing at line: {}", item.getLineNumber());
        }
    }

    private void logSkippedLinesOnFile(InboundTransaction item) {
        try {
            String filename = item.getFilename().replace("\\", "/");
            String[] fileArr = filename.split("/");
            transactionWriterService.write(resolver.getResource(errorTransactionsLogsPath)
                .getFile().getAbsolutePath()
                .concat("/".concat(executionDate))
                + "_" + prefix + "_ErrorRecords_" + fileArr[fileArr.length-1], buildCsv(item));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private String buildCsv(InboundTransaction inboundTransaction) {
        return (Optional.ofNullable(inboundTransaction.getSenderCode()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getOperationType()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getCircuitType()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getPan()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getTrxDate()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getIdTrxAcquirer()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getIdTrxIssuer()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getCorrelationId()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getAmount()).orElse(0L).toString()).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getAmountCurrency()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getAcquirerId()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getMerchantId()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getTerminalId()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getBin()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getMcc()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getFiscalCode()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getVat()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getPosType()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getPar()).orElse("")).concat("\n");
    }

}
