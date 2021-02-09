package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.WriterTrackerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * Implementation of {@link ItemWriter}, to be used for read/processed Transaction files
 */

@Slf4j
@Data
@RequiredArgsConstructor
public class HpanStoreWriter implements ItemWriter<String> {

    private final HpanStoreService hpanStoreService;
    private final Boolean applyHashing;

    @Override
    public void write(List<? extends String> hpanList) {

        hpanList.forEach(hpan-> {
            {
                try {
                    hpanStoreService.store(applyHashing ?
                            DigestUtils.sha256Hex(hpan + hpanStoreService.getSalt()) : hpan);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    log.error("Encountered error on " + hpan);
                }
            }
        });

    }

}
