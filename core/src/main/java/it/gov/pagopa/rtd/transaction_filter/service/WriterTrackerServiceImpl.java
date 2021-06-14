package it.gov.pagopa.rtd.transaction_filter.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@Service
@Data
@RequiredArgsConstructor
@Slf4j
public class WriterTrackerServiceImpl implements WriterTrackerService {

    private final List<CountDownLatch> countDownLatches;

    @SneakyThrows
    @Override
    public synchronized void addCountDownLatch(
            CountDownLatch countDownLatch) {
        countDownLatches.add(countDownLatch);
    }

    @Override
    public synchronized List<CountDownLatch> getCountDownLatches() {
        return countDownLatches;
    }

    @Override
    public void clearAll() {
        countDownLatches.clear();
    }
}
