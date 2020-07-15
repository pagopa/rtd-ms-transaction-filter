package it.gov.pagopa.rtd.transaction_filter.batch.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.nio.charset.Charset;

@Slf4j
@Data
public class TransactionItemReaderListener implements ItemReadListener<InboundTransaction> {

    private String errorTransactionsLogsPath;
    private String executionDate;
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public void beforeRead() {

    }

    public void afterRead(InboundTransaction item) {

        if (log.isDebugEnabled()) {
            log.debug("\n");
            log.debug("####");
            log.debug("Read transaction record on filename :" + item.getFilename() + " ,line: "
                    + item.getLineNumber());
            log.debug("####\n");
        }

    }

    public void onReadError(Exception throwable) {

        if (log.isInfoEnabled()) {
            synchronized (this) {
                log.info("\n");
                log.info("#### Error while reading a transaction record ####");
                log.info(throwable.getMessage());
                log.info("####\n");
            }
        }

        if (throwable instanceof FlatFileParseException) {
            FlatFileParseException flatFileParseException = (FlatFileParseException) throwable;

            try {
                File file = new File(
                resolver.getResource(errorTransactionsLogsPath).getFile().getAbsolutePath()
                                 .concat("/".concat(executionDate))+ "_transactionsErrorRecords.csv");
                FileUtils.writeStringToFile(file, flatFileParseException.getInput().concat("\n"), Charset.defaultCharset(), true);
            } catch (Exception e) {
                log.error(e.getMessage(),e);
            }

        }

    }



}
