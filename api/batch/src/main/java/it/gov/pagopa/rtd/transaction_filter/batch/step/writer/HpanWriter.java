package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.WriterTrackerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Implementation of {@link ItemWriter}, to be used for read/processed Transaction files
 */

@Slf4j
@Data
@RequiredArgsConstructor
public class HpanWriter implements ItemWriter<String> {

    private final HpanStoreService hpanStoreService;
    private final WriterTrackerService writerTrackerService;
    private Executor executor;

    @Override
    public void write(List<? extends String> hpanList) {

        hpanList.forEach(hpan-> {
            {
                try {
                    hpanStoreService.write(hpan);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    log.error("Encountered error on " + hpan);
                }
            }
        });

    }

}
