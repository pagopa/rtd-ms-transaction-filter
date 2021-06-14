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
    public void write(List<? extends String> binRangeList) {

        binRangeList.forEach(binRangeStr -> {
            {
                String[] binRange = binRangeStr.split(";");
                try {
                    binStoreService.store(binRange[0],binRange[1]);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    log.error("Encountered error on " + binRangeList);
                }
            }
        });

    }

}
