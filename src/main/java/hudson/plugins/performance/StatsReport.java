package hudson.plugins.performance;

import org.kohsuke.stapler.Stapler;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Abstract class for classes with size, error, mean, average, 90 line, min and max attributes
 */
public interface StatsReport {
  public int countErrors();
  public double errorPercent();
  public long getAverage();
  public long getMedian();
  public long get90Line();
  public long getMax();
  public long getMin();
  public int size();
}
