package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import it.gov.pagopa.rtd.transaction_filter.service.HpanStoreService;
import it.gov.pagopa.rtd.transaction_filter.service.WriterTrackerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
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
public class HpanWriter implements ItemWriter<String> {

    private final HpanStoreService hpanStoreService;
    private final WriterTrackerService writerTrackerService;
    private final Boolean applyHashing;
    private Executor executor;

    @Override
    public void write(List<? extends String> hpanList) {

        CountDownLatch countDownLatch = new CountDownLatch(hpanList.size());


        hpanList.forEach(hpan-> executor.execute(() ->
                {
                    try {
                        hpanStoreService.store(applyHashing ?
                                DigestUtils.sha256Hex(hpan + hpanStoreService.getSalt()) : hpan);
                    } catch (Exception e) {
                        log.error(e.getMessage(),e);
                        log.error("Encountered error on " + hpan);
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            ));


        writerTrackerService.addCountDownLatch(countDownLatch);
    }

}
