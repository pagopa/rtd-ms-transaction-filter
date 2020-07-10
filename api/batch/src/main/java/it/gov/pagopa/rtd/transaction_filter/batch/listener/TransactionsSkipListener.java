package it.gov.pagopa.rtd.transaction_filter.batch.listener;

import it.gov.pagopa.rtd.transaction_filter.batch.model.InboundTransaction;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import java.io.File;
import java.nio.charset.Charset;

@Slf4j
@Data
public class TransactionsSkipListener implements SkipListener<InboundTransaction, InboundTransaction> {

    private String transactionLogsPath;
    private String executionDate;

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public void onSkipInProcess(InboundTransaction inboundTransaction, Throwable throwable) {

        if (log.isErrorEnabled()) {
            log.info("### Transaction skipped during processing:");
            log.info("model:" + inboundTransaction.toString());
            log.info(throwable.getMessage(),throwable);
        }

    }

    @SneakyThrows
    @Override
    public void onSkipInRead(Throwable throwable) {

        if (log.isErrorEnabled()) {
            log.info("### Transaction skipped during reading:");
            log.info(throwable.getMessage(),throwable);
        }

        if(throwable instanceof FlatFileParseException) {
            FlatFileParseException flatFileParseException = (FlatFileParseException) throwable;
            if (log.isInfoEnabled()) {
                log.info("line:" + flatFileParseException.getLineNumber());
                log.info("input:" + flatFileParseException.getInput());

                try {
                    File file = new File(
                            resolver.getResource(transactionLogsPath).getFile().getAbsolutePath()
                                    .concat(executionDate) + "_transactionsReadErrorRecords.csv");
                    FileUtils.writeStringToFile(file, flatFileParseException.getInput(), Charset.defaultCharset());
                } catch (Exception e) {
                    log.error(e.getMessage(),e);
                }
            }
        }

    }


    @Override
    public void onSkipInWrite(InboundTransaction inboundTransaction, Throwable throwable) {

        if (log.isErrorEnabled()) {
            log.info("### Transaction skipped during writing:");
            log.info(inboundTransaction.toString());
            log.info("model:" + inboundTransaction.toString());
            log.info(throwable.getMessage(),throwable);
        }

    }

}