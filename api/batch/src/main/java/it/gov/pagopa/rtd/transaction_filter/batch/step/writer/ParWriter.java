package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.service.ParStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.WriterTrackerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Implementation of {@link ItemWriter}, to be used for read/processed Par files
 */

@Slf4j
@Data
@RequiredArgsConstructor
public class ParWriter implements ItemWriter<String> {

    private final ParStoreService parStoreService;
    private final WriterTrackerService writerTrackerService;
    private Executor executor;

    @Override
    public void write(List<? extends String> parList) {

        parList.forEach(par-> {
            {
                try {
                    parStoreService.write(par);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    log.error("Encountered error on " + par);
                }
            }
        });

    }

}
