package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.Nullable;

import java.util.Optional;

/**
 * Implementation of {@link ItemProcessListener}, to be used to log and/or store records
 * filtered or that have produced an error during a record processing phase
 */
@Slf4j
@Data
public class TransactionItemProcessListener implements ItemProcessListener<InboundTransaction, InboundTransaction> {

    private String errorTransactionsLogsPath;
    private String executionDate;
    private Boolean enableOnErrorLogging;
    private Boolean enableOnErrorFileLogging;
    private Boolean enableAfterProcessLogging;
    private Boolean enableAfterProcessFileLogging;
    private Long loggingFrequency;
    private TransactionWriterService transactionWriterService;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public void beforeProcess(InboundTransaction inboundTransaction) {}

    public void afterProcess(InboundTransaction item, @Nullable InboundTransaction result) {

        if (enableAfterProcessLogging) {

            if (result == null) {
                if (loggingFrequency > 1 && item.getLineNumber() % loggingFrequency == 0) {
                    log.info("Filtered transaction record on filename: {},line: {}",
                            item.getFilename(),
                            item.getLineNumber());
                } else if (loggingFrequency == 1) {
                    log.debug("Filtered transaction record on filename: {},line: {}",
                            item.getFilename(),
                            item.getLineNumber());
                }

            } else {
                if (loggingFrequency > 1 && item.getLineNumber() % loggingFrequency == 0) {
                    log.info("Processed {} lines on file: {}", item.getLineNumber(), item.getFilename());
                } else if (loggingFrequency == 1) {
                    log.debug("Processed transaction record on filename: {}, line: {}",
                            item.getFilename(), item.getLineNumber());
                }
            }

        }

        if (Boolean.TRUE.equals(enableAfterProcessFileLogging) && result == null) {
            try {
                String file = item.getFilename().replaceAll("\\\\", "/");
                String[] fileArr = file.split("/");
                transactionWriterService.write(resolver.getResource(errorTransactionsLogsPath)
                        .getFile().getAbsolutePath()
                        .concat("/".concat(executionDate))
                        + "_FilteredRecords_"+fileArr[fileArr.length-1]+".csv",buildCsv(item));
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(e.getMessage(), e);
                }
            }
        }

    }

    public void onProcessError(InboundTransaction item, Exception throwable) {

        if (transactionWriterService.hasErrorHpan(item.getFilename()
                .concat(String.valueOf(item.getLineNumber())))) {
            return;
        }

        transactionWriterService.storeErrorPans(item.getFilename()
                .concat(String.valueOf(item.getLineNumber())));

        if (enableOnErrorLogging) {
            log.error("Error during during transaction processing, filename: {},line: {}",
                    item.getFilename(), item.getLineNumber());
        }

        if (enableOnErrorFileLogging) {
            try {
                String filename = item.getFilename().replaceAll("\\\\", "/");
                String[] fileArr = filename.split("/");
                transactionWriterService.write(resolver.getResource(errorTransactionsLogsPath)
                        .getFile().getAbsolutePath()
                        .concat("/".concat(executionDate))
                        + "_ErrorRecords_"+fileArr[fileArr.length-1]+".csv",buildCsv(item));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

    }

    private String buildCsv(InboundTransaction inboundTransaction) {
        return (Optional.ofNullable(inboundTransaction.getAcquirerCode()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getOperationType()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getCircuitType()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getPan()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getTrxDate()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getIdTrxAcquirer()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getIdTrxIssuer()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getCorrelationId()).orElse("")).concat(";")
                .concat(inboundTransaction.getAmount() != null ? inboundTransaction.getAmount().toString() : "").concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getAmountCurrency()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getAcquirerId()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getMerchantId()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getTerminalId()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getBin()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getMcc()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getVat()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getPosType()).orElse("")).concat(";")
                .concat(Optional.ofNullable(inboundTransaction.getPar()).orElse("")).concat("\n");
    }

}
