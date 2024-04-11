package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * Implementation of {@link ItemWriter}, to be used for read/processed Transaction files
 */

@RequiredArgsConstructor
public class HpanWriter implements ItemWriter<String> {

    private final StoreService storeService;
    private final Boolean applyHashing;

    @Override
    public void write(Chunk<? extends String> hpanList) {
        hpanList.forEach(hpan-> storeService.store(Boolean.TRUE.equals(applyHashing) ?
                DigestUtils.sha256Hex(hpan+ storeService.getSalt()) : hpan));
    }

}
