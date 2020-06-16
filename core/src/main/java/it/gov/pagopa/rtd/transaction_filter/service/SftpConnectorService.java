package it.gov.pagopa.rtd.transaction_filter.service;

import java.io.File;

public interface SftpConnectorService {

    void transferFile(File file);

}
