package it.gov.pagopa.rtd.transaction_filter.connector;

import it.gov.pagopa.rtd.transaction_filter.connector.model.FileReport;

public interface FileReportRestClient {

  FileReport getFileReport(int daysAgo);
}
