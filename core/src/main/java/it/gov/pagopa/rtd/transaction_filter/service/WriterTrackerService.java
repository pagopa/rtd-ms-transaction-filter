package it.gov.pagopa.rtd.transaction_filter.service;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public interface WriterTrackerService {

    void addCountDownLatch(CountDownLatch countDownLatch);

    List<CountDownLatch> getCountDownLatches();

    void clearAll();

}
