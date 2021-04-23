package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTokenPan;
import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

/**
 * Implementation of {@link ItemWriteListener}, to be used to log and/or store records
 * that have produced an error while reading a record writing phase
 */

@Slf4j
@Data
public class TokenItemWriterListener implements ItemWriteListener<InboundTokenPan> {

    private String errorTransactionsLogsPath;
    private String executionDate;
    private Boolean enableOnErrorLogging;
    private Boolean enableOnErrorFileLogging;
    private Boolean enableAfterWriteLogging;
    private Long loggingFrequency;
    private TransactionWriterService transactionWriterService;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public void beforeWrite(List<? extends InboundTokenPan> list) {

    }

    public void afterWrite(List<? extends InboundTokenPan> inboundTokenPans) {
        if (enableAfterWriteLogging) {
            inboundTokenPans.forEach(inboundTokenPan -> {
                if (loggingFrequency > 1 && inboundTokenPan.getLineNumber() % loggingFrequency == 0) {
                    log.info("Written {} lines on file: {}",
                            inboundTokenPan.getLineNumber(), inboundTokenPan.getFilename());
                } else if (loggingFrequency == 1) {
                    log.debug("Written token record on filename: {}, line: {}",
                            inboundTokenPan.getFilename(), inboundTokenPan.getLineNumber());
                }
            });
        }
    }

    public void onWriteError(Exception throwable, List<? extends InboundTokenPan> inboundTokenPans) {

        inboundTokenPans.forEach(inboundTokenPan -> {

            if (enableOnErrorLogging) {
                log.error("Error during during token record writing - {},filename: {},line: {}",
                        throwable.getMessage(), inboundTokenPan.getFilename(), inboundTokenPan.getLineNumber());
            }

            if (enableOnErrorFileLogging) {
                try {
                    String filename = inboundTokenPan.getFilename().replaceAll("\\\\", "/");
                    String[] fileArr = filename.split("/");
                    transactionWriterService.write(resolver.getResource(errorTransactionsLogsPath)
                            .getFile().getAbsolutePath()
                            .concat("/".concat(executionDate))
                            + "_FilteredRecords_"+fileArr[fileArr.length-1]+".csv",buildTokenPan(inboundTokenPan));

                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

        });


    }

    private String buildTokenPan(InboundTokenPan inboundTokenPan) {
        return (inboundTokenPan.getTokenPan() != null ? inboundTokenPan.getTokenPan() : "").concat(";")
                .concat(inboundTokenPan.getPar() != null ? inboundTokenPan.getPar() : "").concat("\n");
    }

}
