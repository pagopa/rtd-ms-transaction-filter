package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.batch.utils.TransactionMaskPolicy;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link ItemWriteListener}, to be used to log and/or store records
 * that have produced an error while reading a record writing phase
 */

@Slf4j
@Data
public class TransactionItemWriterListener implements ItemWriteListener<InboundTransaction> {

    private String errorTransactionsLogsPath;
    private String executionDate;
    private Boolean enableOnErrorLogging;
    private Boolean enableOnErrorFileLogging;
    private Boolean enableAfterWriteLogging;
    private Long loggingFrequency;
    private TransactionWriterService transactionWriterService;
    private String prefix;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final TransactionMaskPolicy maskPolicy;

    public void afterWrite(List<? extends InboundTransaction> inboundTransactions) {
        if (Boolean.TRUE.equals(enableAfterWriteLogging)) {
            inboundTransactions.forEach(inboundTransaction -> {
                if (loggingFrequency > 1 && inboundTransaction.getLineNumber() % loggingFrequency == 0) {
                    log.info("Written {} lines on file: {}",
                            inboundTransaction.getLineNumber(), inboundTransaction.getFilename());
                } else if (loggingFrequency == 1) {
                    log.debug("Written transaction record on filename: {}, line: {}",
                            inboundTransaction.getFilename(), inboundTransaction.getLineNumber());
                }
            });
        }
    }

    public void onWriteError(Exception throwable, List<? extends InboundTransaction> inboundTransactions) {

        inboundTransactions.forEach(inboundTransaction -> {

            if (Boolean.TRUE.equals(enableOnErrorLogging)) {
                log.error("Error during during transaction record writing - {},filename: {},line: {}",
                        throwable.getMessage(), inboundTransaction.getFilename(), inboundTransaction.getLineNumber());
            }

            if (Boolean.TRUE.equals(enableOnErrorFileLogging)) {
                maskPolicy.apply(inboundTransaction);
                try {
                    String filename = inboundTransaction.getFilename().replace("\\", "/");
                    String[] fileArr = filename.split("/");
                    transactionWriterService.write(resolver.getResource(errorTransactionsLogsPath)
                            .getFile().getAbsolutePath()
                            .concat("/".concat(executionDate))
                            + "+" + prefix + "_FilteredRecords_"+fileArr[fileArr.length-1],buildCsv(inboundTransaction));

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

        });


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
