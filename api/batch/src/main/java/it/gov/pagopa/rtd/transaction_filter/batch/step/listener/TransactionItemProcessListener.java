package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Implementation of {@link ItemProcessListener}, to be used to log and/or store records
 * filtered or that have produced an error during a record processing phase
 */
@Slf4j
@Data
public class TransactionItemProcessListener implements ItemProcessListener<InboundTransaction,InboundTransaction> {

    private String errorTransactionsLogsPath;
    private String executionDate;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public void beforeProcess(InboundTransaction inboundTransaction) {

    }

    public void afterProcess(InboundTransaction item, @Nullable InboundTransaction result) {

        if (result == null) {

            if (log.isInfoEnabled()) {
                    log.info("Filtered transaction record on filename: "
                            + item.getFilename() + " ,line: " +
                            item.getLineNumber());
            }

            try {
                File file = new File(
                        resolver.getResource(errorTransactionsLogsPath).getFile().getAbsolutePath()
                                .concat("/".concat(executionDate)) + "_transactionsFilteredRecords.csv");
                FileUtils.writeStringToFile(file, buildCsv(item), Charset.defaultCharset(), true);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(e.getMessage(), e);
                }
            }

        } else {
            if (log.isDebugEnabled()) {
                log.debug("Processed transaction record on filename: " + item.getFilename() + " ,line: " +
                        item.getLineNumber());
            }
        }

    }

    public void onProcessError(InboundTransaction item, Exception throwable) {

        if (log.isInfoEnabled()) {
                log.info("Error during during transaction processing, filename: " +
                        item.getFilename() + ",line: " + item.getLineNumber());
        }

        try {
            File file = new File(
                    resolver.getResource(errorTransactionsLogsPath).getFile().getAbsolutePath()
                            .concat("/".concat(executionDate)) + "_transactionsErrorRecords.csv");
            FileUtils.writeStringToFile(file,buildCsv(item) , Charset.defaultCharset(), true);
        } catch (Exception e) {
            log.error(e.getMessage(),e);
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
                .concat(inboundTransaction.getMcc() != null ? inboundTransaction.getMcc() : "").concat("\n");
    }

}
