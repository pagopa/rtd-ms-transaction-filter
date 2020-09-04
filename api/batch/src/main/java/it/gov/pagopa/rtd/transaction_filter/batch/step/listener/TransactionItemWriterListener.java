package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Implementation of {@link ItemWriteListener}, to be used to log and/or store records
 * that have produced an error while reading a record writing phase
 */

@Slf4j
@Data
public class TransactionItemWriterListener implements ItemWriteListener<InboundTransaction> {

    private String errorTransactionsLogsPath;
    private String executionDate;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public void beforeWrite(List<? extends InboundTransaction> list) {

    }

    public void afterWrite(List<? extends InboundTransaction> inboundTransactions) {
        if (log.isDebugEnabled()) {
            inboundTransactions.forEach(inboundTransaction -> {
                log.debug("Transaction record from filename: "
                        + inboundTransaction.getFilename() + " ,line: "
                        + inboundTransaction.getLineNumber() +" written");
            });
        }
    }

    public void onWriteError(Exception throwable, List<? extends InboundTransaction> inboundTransactions) {

        inboundTransactions.forEach(inboundTransaction -> {

            if (log.isInfoEnabled()) {
                log.info("Error during during transaction record writing - " + throwable.getMessage() + ", filename: "
                        + inboundTransaction.getFilename() + ",line: " + inboundTransaction.getLineNumber());
            }

            try {
                File file = new File(
                        resolver.getResource(errorTransactionsLogsPath).getFile().getAbsolutePath()
                                .concat("/".concat(executionDate)) + "_transactionsErrorRecords.csv");
                FileUtils.writeStringToFile(
                        file, buildCsv(inboundTransaction), Charset.defaultCharset(), true);

            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(e.getMessage(), e);
                }
            }

        });


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
                .concat(inboundTransaction.getMcc() != null ? inboundTransaction.getMcc() : "").concat("\n");
    }

}
