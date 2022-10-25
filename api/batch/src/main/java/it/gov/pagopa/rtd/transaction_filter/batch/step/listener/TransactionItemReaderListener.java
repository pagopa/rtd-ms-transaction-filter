package it.gov.pagopa.rtd.transaction_filter.batch.step.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import it.gov.pagopa.rtd.transaction_filter.service.TransactionWriterService;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;


/**
 * Implementation of {@link ItemReadListener}, to be used to log and/or store records
 * that have produced an error while reading a record
 */

@Slf4j
@Data
public class TransactionItemReaderListener implements ItemReadListener<InboundTransaction> {

    private String errorTransactionsLogsPath;
    private String executionDate;
    private Boolean enableOnErrorLogging;
    private Boolean enableOnErrorFileLogging;
    private Boolean enableAfterReadLogging;
    private Long loggingFrequency;
    private TransactionWriterService transactionWriterService;
    private String prefix;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public void beforeRead() {
        // do nothing
    }

    public void afterRead(InboundTransaction item) {

        if (Boolean.TRUE.equals(enableAfterReadLogging)) {
            if (loggingFrequency > 1 && item.getLineNumber() % loggingFrequency == 0) {
                log.info("Read {} lines on file: {}", item.getLineNumber(), item.getFilename());
            } else if (loggingFrequency == 1) {
                log.debug("Read transaction record on filename: {}, line: {}",
                        item.getFilename(), item.getLineNumber());
            }
        }

    }

    @SneakyThrows
    public void onReadError(Exception throwable) {

        if (Boolean.TRUE.equals(enableOnErrorLogging)) {
        log.error("Error while reading a transaction record - {} - {}", throwable.getMessage(),
            getStringNotParsedIfPresent(throwable.getCause()));
        }

        if (Boolean.TRUE.equals(enableOnErrorFileLogging) && throwable instanceof FlatFileParseException) {
            FlatFileParseException flatFileParseException = (FlatFileParseException) throwable;
            String filename =  flatFileParseException.getMessage().split("\\[",3)[2]
                    .replace("]","").replace("\\\\", "/");
            String[] fileArr = filename.split("/");
            try {
                String lineContent = flatFileParseException.getInput();
                transactionWriterService.write(resolver.getResource(errorTransactionsLogsPath)
                        .getFile().getAbsolutePath()
                        .concat("/".concat(executionDate))
                        + "_" + prefix + "_FilteredRecords_" + fileArr[fileArr.length-1],
                        lineContent.concat("\n"));
            } catch (Exception e) {
                log.error(e.getMessage(),e);
            }

        }

    }

    private String getStringNotParsedIfPresent(Throwable exception) {
        if (exception != null && exception.getCause() != null) {
            return exception.getCause().getMessage();
        }
        return "";
    }
}
