package it.gov.pagopa.rtd.transaction_filter.batch.step.reader;

import java.util.Iterator;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.lang.Nullable;

public class CustomIteratorItemReader<T> implements ItemReader<T> {
  private Iterable<T> iterable;
  private Iterator<T> iterator;

  public CustomIteratorItemReader(Iterable<T> iterable) {
    this.iterable = iterable;
  }

  @BeforeStep
  public void initializeState(StepExecution stepExecution) {
    this.iterator = null;
  }

  @Nullable
  public synchronized T read() {
    if (this.iterator == null) {
      this.iterator = this.iterable.iterator();
    }
    return this.iterator.hasNext() ? this.iterator.next() : null;
  }
}
