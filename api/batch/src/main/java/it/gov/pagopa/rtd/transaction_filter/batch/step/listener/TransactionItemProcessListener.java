package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.Nullable;

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

        if (enableAfterProcessFileLogging && result == null) {
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
        return (inboundTransaction.getAcquirerCode() != null ? inboundTransaction.getAcquirerCode() : "").concat(";")
                .concat(inboundTransaction.getOperationType() != null ? inboundTransaction.getOperationType() : "").concat(";")
                .concat(inboundTransaction.getCircuitType() != null ? inboundTransaction.getCircuitType() : "").concat(";")
                .concat(inboundTransaction.getPan() != null ? inboundTransaction.getPan() : "").concat(";")
                .concat(inboundTransaction.getTrxDate() != null ? inboundTransaction.getTrxDate() : "").concat(";")
                .concat(inboundTransaction.getIdTrxAcquirer() != null ? inboundTransaction.getIdTrxAcquirer() : "").concat(";")
                .concat(inboundTransaction.getIdTrxIssuer() != null ? inboundTransaction.getIdTrxIssuer() : "").concat(";")
                .concat(inboundTransaction.getCorrelationId() != null ? inboundTransaction.getCorrelationId() : "").concat(";")
                .concat(inboundTransaction.getAmount() != null ? inboundTransaction.getAmount().toString() : "").concat(";")
                .concat(inboundTransaction.getAmountCurrency() != null ? inboundTransaction.getAmountCurrency() : "").concat(";")
                .concat(inboundTransaction.getAcquirerId() != null ? inboundTransaction.getAcquirerId() : "").concat(";")
                .concat(inboundTransaction.getMerchantId() != null ? inboundTransaction.getMerchantId() : "").concat(";")
                .concat(inboundTransaction.getTerminalId() != null ? inboundTransaction.getTerminalId() : "").concat(";")
                .concat(inboundTransaction.getBin() != null ? inboundTransaction.getBin() : "").concat(";")
                .concat(inboundTransaction.getMcc() != null ? inboundTransaction.getMcc() : "").concat(";")
                .concat(inboundTransaction.getPar() != null ? inboundTransaction.getPar() : "").concat("\n");
    }

}
