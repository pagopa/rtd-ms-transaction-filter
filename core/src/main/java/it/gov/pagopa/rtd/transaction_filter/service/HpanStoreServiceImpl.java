package it.gov.pagopa.rtd.transaction_filter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Slf4j
class HpanStoreServiceImpl implements HpanStoreService {

    private final TreeSet<String> hpanSet;

    @Override
    public synchronized void store(String hpan) {
        hpanSet.add(hpan);
    }

    @Override
    public Boolean hasHpan(String hpan) {
        return hpanSet.contains(hpan);
    }

    @Override
    public void clearAll() {
        hpanSet.clear();
    }


}
