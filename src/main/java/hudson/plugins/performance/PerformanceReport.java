package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Represents a single performance report, which consists of multiple {@link UriReport}s for
 * different URLs that was tested.
 *
 * This object belongs under {@link PerformanceReportMap}.
 */
public class PerformanceReport extends AbstractReport
    implements Comparable<PerformanceReport>, StatsReport {

  private PrintStream errorStream;

  private String reportFileName = null;

  private AggregateStatistics stats = new AggregateStatistics.Unfrozen();

  /**
   * {@link AbstractBuild} that this performance report is associated with
   */
  private transient AbstractBuild<?, ?> build;

  /**
   * {@link PerformanceReportParser} that is used to parse this report
   */
  private transient PerformanceReportParser parser;

  /**
   * The source {@link File} for this report
   */
  private transient File sourceFile;

  /**
   * The {@link TaskListener} for the task where this report was loaded
   */
  private transient TaskListener listener;

  /**
   * {@link UriReport}s keyed by their {@link UriReport#getStaplerUri()}.
   */
  private final Map<String, UriReport> uriReportMap = new LinkedHashMap<String, UriReport>();

  public void addSample(HttpSample pHttpSample) throws SAXException {
    String uri = pHttpSample.getUri();
    if (uri == null) {
      getErrorStream().println(
          "label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");
      return;
    }
    String staplerUri = uri.replace("http:", "").replaceAll("/", "_");
    UriReport uriReport = uriReportMap.get(staplerUri);
    if (uriReport == null) {
      uriReport = new UriReport(this, staplerUri, uri);
      uriReportMap.put(staplerUri, uriReport);
    }
    uriReport.addHttpSample(pHttpSample);
    AggregateStatistics.Unfrozen ufstats = stats.asUnfrozen();
    ufstats.sample(pHttpSample.getDuration(), !pHttpSample.isSuccessful());
    stats = ufstats;
  }

  public int compareTo(PerformanceReport jmReport) {
    if (this == jmReport) {
      return 0;
    }
    return getReportFileName().compareTo(jmReport.getReportFileName());
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
    return Messages.Report_DisplayName();
  }

  public UriReport getDynamic(String token) throws IOException {
    return uriReportMap.get(token);
  }

  public String getReportFileName() {
    return reportFileName;
  }

  public AbstractBuild<?, ?> getBuild() {
    return build;
  }

  public void setBuild(AbstractBuild<?, ?> build) {
    this.build = build;
  }

  public void setParser(PerformanceReportParser parser) {
    this.parser = parser;
  }

  public PerformanceReportParser getParser() {
    return parser;
  }

  public void setSourceFile(File source) {
    this.sourceFile = source;
  }

  public File getSourceFile() {
    return sourceFile;
  }

  public void setListener(TaskListener listener) {
    this.listener = listener;
  }

  public TaskListener getListener() {
    return listener;
  }

  public PrintStream getErrorStream() {
    return errorStream;
  }

  public void setErrorStream(PrintStream stream) {
    errorStream = stream;
  }

  public List<UriReport> getUriListOrdered() {
    Collection<UriReport> uriCollection = uriReportMap.values();
    List<UriReport> UriReportList = new ArrayList<UriReport>(uriCollection);
    return UriReportList;
  }

  public Map<String, UriReport> getUriReportMap() {
    return uriReportMap;
  }

  public void setReportFileName(String reportFileName) {
    this.reportFileName = reportFileName;
  }
}
