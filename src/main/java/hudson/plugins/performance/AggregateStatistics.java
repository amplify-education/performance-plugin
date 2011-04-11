package hudson.plugins.performance;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Computes a set of aggregate statistics incrementally over
 * a set of sample values
 */
public abstract class AggregateStatistics {

  protected long total = 0;
  protected int size = 0;
  protected long min = Long.MAX_VALUE;
  protected long max = Long.MIN_VALUE;
  protected int errors = 0;

  public static class Frozen extends AggregateStatistics {
    private long line90 = 0;
    private long median = 0;

    public Frozen(AggregateStatistics.Unfrozen stats) {
      this.total = stats.getTotal();
      this.size = stats.getSize();
      this.min = stats.getMin();
      this.max = stats.getMax();
      this.errors = stats.getErrorCount();
      this.line90 = stats.get90Line();
      this.median = stats.getMedian();
    }

    @Override
    public long get90Line() {
      return line90;
    }

    @Override
    public long getMedian() {
      return median;
    }

    @Override
    public Frozen asFrozen() {
      return this;
    }

    @Override
    public Unfrozen asUnfrozen() {
      return new Unfrozen();
    }
  }

  public static class Unfrozen extends AggregateStatistics {
    private Long line90 = Long.valueOf(0);
    private Long median = Long.valueOf(0);
    private boolean samplesSorted = true;
    private List<Long> samples = new ArrayList<Long>();

    @Override
    public long get90Line() {
      if (line90 == null) {
        line90 = getPercentile(.9);
      }
      return line90;
    }

    @Override
    public long getMedian() {
      if (median == null) {
        median = getPercentile(.5);
      }
      return median;
    }

    private long getPercentile(double percentile) {
      if (!samplesSorted) {
        Collections.sort(samples);
        samplesSorted = true;
      }
      return samples.get((int) (size*percentile));
    }

    public void sample(long sample, boolean error) {
      total += sample;
      size += 1;
      max = Math.max(max, sample);
      min = Math.min(min, sample);
      samples.add(sample);
      if (error) {
        errors += 1;
      }
      samplesSorted = false;
      line90 = null;
      median = null;
    }

    @Override
    public Frozen asFrozen() {
      return new Frozen(this);
    }

    @Override
    public Unfrozen asUnfrozen() {
      return this;
    }

    public static class UnfrozenConverter implements Converter {
      public boolean canConvert(Class clazz) {
        return Unfrozen.class == clazz;
      }

      public void marshal(Object value, HierarchicalStreamWriter writer,
          MarshallingContext context) {
        context.convertAnother(((Unfrozen)value).asFrozen());
      }

      public Object unmarshal(HierarchicalStreamReader reader,
          UnmarshallingContext context) {
        Unfrozen unfrozen = new Unfrozen();
        return (Frozen)context.convertAnother(unfrozen, Frozen.class);
      }
    }
  }

  public long getTotal() {
    return total;
  }

  public int getErrorCount() {
    return errors;
  }

  public double getErrorPercent() {
    return size == 0 ? 0 : (double)errors / size;
  }

  public double getAverage() {
    return size == 0 ? 0 : (double)total / size;
  }

  abstract public long get90Line();
  abstract public long getMedian();

  public long getMax() {
    return max;
  }

  public long getMin() {
    return min;
  }

  public int getSize() {
    return size;
  }

  abstract public Frozen asFrozen();
  abstract public Unfrozen asUnfrozen();
  
}
