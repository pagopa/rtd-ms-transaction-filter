package it.gov.pagopa.rtd.transaction_filter.service;

import java.io.File;

public interface HpanConnectorService {

    File getHpanList();

    String getSalt();
}
