package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.service.BinStoreService;
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
public class BinStoreWriter implements ItemWriter<String> {

    private final BinStoreService binStoreService;

    @Override
    public void write(List<? extends String> birList) {

        birList.forEach(bin -> {
            {
                try {
                    binStoreService.store(bin);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    log.error("Encountered error on " + bin);
                }
            }
        });

    }

}
