package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A report about a particular tested URI.
 * 
 * This object belongs under {@link PerformanceReport}.
 */
public class UriReport extends AbstractReport implements ModelObject,
    Comparable<UriReport> {

  public final static String END_PERFORMANCE_PARAMETER = ".endperformanceparameter";

  /**
   * Individual HTTP invocations to this URI and how they went.
   */
  private transient final List<HttpSample> httpSampleList = new ArrayList<HttpSample>();

  /**
   * The parent object to which this object belongs.
   */
  private final PerformanceReport performanceReport;

  /**
   * Escaped {@link #uri} that doesn't contain any letters that cannot be used
   * as a token in URL.
   */
  private final String staplerUri;

  private  AggregateStatistics stats = new AggregateStatistics.Unfrozen();

  private String uri;

  UriReport(PerformanceReport performanceReport, String staplerUri, String uri) {
    this.performanceReport = performanceReport;
    this.staplerUri = staplerUri;
    this.uri = uri;
  }

  public void addHttpSample(HttpSample httpSample) {
    httpSampleList.add(httpSample);
    AggregateStatistics.Unfrozen ufstats = stats.asUnfrozen();
    ufstats.sample(httpSample.getDuration(), !httpSample.isSuccessful());
    stats = ufstats;
  }

  public int compareTo(UriReport uriReport) {
    if (uriReport == this) {
      return 0;
    }
    return uriReport.getUri().compareTo(this.getUri());
  }

  public int countErrors() {
    return stats.getErrorCount();
  }

  public double errorPercent() {
    return stats.getErrorPercent();
  }

  public long getAverage() {
    return (long)stats.getAverage();
  }

  public long get90Line() {
    return stats.get90Line();
  }

  public long getMedian() {
    return stats.getMedian();
  }

  public long getMax() {
    return stats.getMax();
  }

  public long getMin() {
    return stats.getMin();
  }

  public int size() {
    return stats.getSize();
  }

  public String getDisplayName() {
    return getUri();
  }

  public PerformanceReport getPerformanceReport() {
    return performanceReport;
  }

  public AbstractBuild<?, ?> getBuild() {
    return performanceReport.getBuild();
  }

  public String getStaplerUri() {
    return staplerUri;
  }

  public String getUri() {
    return uri;
  }

  public boolean isFailed() {
    return countErrors() != 0;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
 
  public List<HttpSample> getHttpSamples() {
    return Collections.unmodifiableList(httpSampleList);
  }

  public boolean hasHttpSamples() {
    return httpSampleList != null && httpSampleList.size() == size();
  }

  public String encodeUriReport() throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder(120);
    sb.append(performanceReport.getReportFileName()).append(
        GraphConfigurationDetail.SEPARATOR).append(getStaplerUri()).append(
        END_PERFORMANCE_PARAMETER);
    return URLEncoder.encode(sb.toString(), "UTF-8");
  }

}
