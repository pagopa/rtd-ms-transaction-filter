package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

/**
 * Implementation of {@link ItemWriter}, to be used for read/processed Transaction files
 */

@Slf4j
@RequiredArgsConstructor
public class HpanWriter implements ItemWriter<String> {

    private final HpanStoreService hpanStoreService;
    private final Boolean applyHashing;

    @Override
    public void write(List<? extends String> hpanList) {
        hpanList.stream().forEach(hpan-> {
            hpanStoreService.store(applyHashing ?
                    DigestUtils.sha256Hex(hpan+hpanStoreService.getSalt()) : hpan);
        });
    }

}
