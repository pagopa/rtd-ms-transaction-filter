package it.gov.pagopa.rtd.transaction_filter.batch.step.reader;

import java.util.Iterator;
import org.springframework.batch.item.ItemReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class CustomIteratorItemReader<T> implements ItemReader<T> {
  private Iterable<T> iterable;
  private Iterator<T> iterator;

  public CustomIteratorItemReader(Iterable<T> iterable) {
    Assert.notNull(iterable, "Iterable argument cannot be null!");
    this.iterable = iterable;
  }

  @Nullable
  public synchronized T read() {
    if (this.iterator == null) {
      this.iterator = this.iterable.iterator();
    }
    return this.iterator.hasNext() ? this.iterator.next() : null;
  }
}
