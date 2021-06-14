package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.service.ParStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.TokenPanStoreService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * Implementation of {@link ItemWriter}, to be used for read/processed Par files
 */

@Slf4j
@Data
@RequiredArgsConstructor
public class TokenPanStoreWriter implements ItemWriter<String> {

    private final TokenPanStoreService tokenPanStoreService;

    @Override
    public void write(List<? extends String> tokenPanList) {

        tokenPanList.forEach(tokenPan -> {
            {
                try {
                    tokenPanStoreService.store(tokenPan);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    log.error("Encountered error on " + tokenPan);
                }
            }
        });

    }

}
