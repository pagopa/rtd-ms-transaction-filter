package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
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
public class TokenItemProcessListener implements ItemProcessListener<InboundTokenPan, InboundTokenPan> {

    private String tokenPanInputPath;
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
    public void beforeProcess(InboundTokenPan inboundTokenPan) {}

    public void afterProcess(InboundTokenPan item, @Nullable InboundTokenPan result) {

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
                        + "_FilteredRecords_"+fileArr[fileArr.length-1]+".csv",buildTokenPan(item));

            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(e.getMessage(), e);
                }
            }
        }

    }

    public void onProcessError(InboundTokenPan item, Exception throwable) {

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
                        + "_ErrorRecords_"+fileArr[fileArr.length-1]+".csv",buildTokenPan(item));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

    }

    private String buildTokenPan(InboundTokenPan inboundTokenPan) {
        return (inboundTokenPan.getTokenPan() != null ? inboundTokenPan.getTokenPan() : "").concat(";")
                .concat(inboundTokenPan.getCircuitType() != null ? inboundTokenPan.getCircuitType() : "").concat(";")
                .concat(inboundTokenPan.getPar() != null ? inboundTokenPan.getPar() : "").concat("\n");
    }

}
