package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.service.TokenPanStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.WriterTrackerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import java.util.List;

/**
 * Implementation of {@link ItemWriter}, to be used for read/processed Transaction files
 */

@Slf4j
@Data
@RequiredArgsConstructor
public class TokenPanWriter implements ItemWriter<String> {

    private final TokenPanStoreService tokenPanStoreService;
    private final WriterTrackerService writerTrackerService;

    @Override
    public void write(List<? extends String> tokenPanList) {

        tokenPanList.forEach(tokenPan-> {
            {
                try {
                    tokenPanStoreService.write(tokenPan);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    log.error("Encountered error on " + tokenPan);
                }
            }
        });

    }

}
