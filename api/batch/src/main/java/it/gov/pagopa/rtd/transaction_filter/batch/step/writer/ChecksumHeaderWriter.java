package it.gov.pagopa.rtd.transaction_filter.batch.step.writer;

import org.springframework.batch.item.file.FlatFileHeaderCallback;

import java.io.IOException;
import java.io.Writer;

public class ChecksumHeaderWriter implements FlatFileHeaderCallback {

  private final String header;

  public ChecksumHeaderWriter(String header) {
    this.header = header;
  }

  @Override
  public void writeHeader(Writer writer) throws IOException {
    writer.write("#sha256sum:" + header);
  }
}
