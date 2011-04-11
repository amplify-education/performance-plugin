package hudson.plugins.performance;

import hudson.FilePath;

import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.ModelObject;
import hudson.model.TaskListener;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Root object of a performance report.
 */
public class PerformanceReportMap implements ModelObject {

  private static final Logger LOGGER = Logger.getLogger(PerformanceReportMap.class.getName());

  /**
   * The {@link PerformanceBuildAction} that this report belongs to.
   */
  private transient PerformanceBuildAction buildAction;

  /**
   * {@link PerformanceReport}s are keyed by {@link PerformanceReport#reportFileName}
   *
   * Test names are arbitrary human-readable and URL-safe string that identifies an individual report.
   */
  private Map<String, PerformanceReport> performanceReportMap = new LinkedHashMap<String, PerformanceReport>();

  private static final String PERFORMANCE_REPORTS_DIRECTORY = "performance-reports";
  private static final String PERFORMANCE_SUMMARIES_DIRECTORY = "performance-summaries";

  /**
   * Parses the reports and build a {@link PerformanceReportMap}.
   *
   * @throws IOException
   *      If a report fails to parse.
   */
  PerformanceReportMap(PerformanceBuildAction buildAction, TaskListener listener)
      throws IOException, InterruptedException {
    this.buildAction = buildAction;

    File repo = new File(getBuild().getRootDir(),
        PerformanceReportMap.getPerformanceReportDirRelativePath());

    // files directly under the directory are for JMeter, for compatibility reasons.
    File[] files = repo.listFiles(new FileFilter() {
      public boolean accept(File f) {
        return !f.isDirectory();
      }
    });
    // this may fail, if the build itself failed, we need to recover gracefully
    if (files != null) {
      loadAll(Arrays.asList(files), new JMeterParser(""), listener);
    }

    // otherwise subdirectory name designates the parser ID.
    File[] dirs = repo.listFiles(new FileFilter() {
      public boolean accept(File f) {
        return f.isDirectory();
      }
    });
    // this may fail, if the build itself failed, we need to recover gracefully
    if (dirs != null) {
      for (File dir : dirs) {
        PerformanceReportParser p = buildAction.getParserById(dir.getName());
        if (p != null) {
          loadAll(Arrays.asList(dir.listFiles()), p, listener);
        }
      }
    }
  }

  private void loadAll(Collection<File> files, PerformanceReportParser parser, TaskListener listener)
      throws IOException, InterruptedException {
    for (File f: files) {
      try {
        PerformanceReport report = loadPerformanceReport(f, parser, listener);
        performanceReportMap.put(report.getReportFileName(), report);
      } catch (PerformanceReportParser.ParseException exc) {
        // Don't add a report that won't parse to the results
      }
    }
  }

  private PerformanceReport loadPerformanceReport(File reportFile, PerformanceReportParser parser, TaskListener listener)
      throws IOException, PerformanceReportParser.ParseException, InterruptedException {
    final Hudson app = Hudson.getInstance();
    FilePath summaryReport = new FilePath(new File(reportFile.toString().replaceFirst(
      getPerformanceReportDirRelativePath(),
      getPerformanceSummaryDirRelativePath()
    )));
    Hudson.XSTREAM.registerConverter(new AggregateStatistics.Unfrozen.UnfrozenConverter());
    PerformanceReport report = null;
    try {
      report = (PerformanceReport)Hudson.XSTREAM.fromXML(summaryReport.read());
    } catch (FileNotFoundException exc) {
      report = parser.parse(getBuild(), reportFile, listener);
      Hudson.XSTREAM.toXML(report, summaryReport.write());
    }
    report.setBuild(getBuild());
    report.setParser(parser);
    report.setSourceFile(reportFile);
    report.setListener(listener);
    return report;
  }

  public AbstractBuild<?, ?> getBuild() {
    return buildAction.getBuild();
  }

  PerformanceBuildAction getBuildAction() {
    return buildAction;
  }

  public String getDisplayName() {
    return Messages.Report_DisplayName();
  }

  public List<PerformanceReport> getPerformanceListOrdered() {
    List<PerformanceReport> listPerformance = new ArrayList<PerformanceReport>(
        getPerformanceReportMap().values());
    Collections.sort(listPerformance);
    return listPerformance;
  }

  public Map<String, PerformanceReport> getPerformanceReportMap() {
    return performanceReportMap;
  }

  /**
   * <p>
   * Give the Performance report with the parameter for name in Bean
   * </p>
   * 
   * @param performanceReportName
   * @return
   */
  public PerformanceReport getPerformanceReport(String performanceReportName) {
    return performanceReportMap.get(performanceReportName);
  }

  /**
   * Get a URI report within a Performance report file (using a full parse)
   * 
   * @param uriReport
   *            "Performance report file name";"URI name"
   * @return
   */
  public UriReport getUriReport(String uriReport) {
    if (uriReport != null) {
      String uriReportDecoded;
      try {
        uriReportDecoded = URLDecoder.decode(uriReport.replace(
            UriReport.END_PERFORMANCE_PARAMETER, ""), "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
        return null;
      }
      StringTokenizer st = new StringTokenizer(uriReportDecoded,
          GraphConfigurationDetail.SEPARATOR);
      String filename = st.nextToken();
      String uri = st.nextToken();
      Map<String, PerformanceReport> reportMap = getPerformanceReportMap();
      PerformanceReport perfReport = reportMap.get(filename);
      UriReport uriPerfReport = perfReport.getUriReportMap().get(uri);
      if (uriPerfReport.hasHttpSamples()) {
        return uriPerfReport;
      } else {
        try {
          PerformanceReport parsed = perfReport.getParser().parse(
            perfReport.getBuild(),
            perfReport.getSourceFile(),
            perfReport.getListener()
          );
          reportMap.put(parsed.getReportFileName(), parsed);
          return parsed.getUriReportMap().get(uri);
        } catch (IOException exc) {
          LOGGER.log(Level.SEVERE, "Unable to re-parse for uri report " + uriReport, exc);
          return uriPerfReport;
        } catch (PerformanceReportParser.ParseException exc) {
          LOGGER.log(Level.SEVERE, "Unable to re-parse for uri report " + uriReport, exc);
          return uriPerfReport;
        }
      }
    } else {
      return null;
    }
  }

  public String getUrlName() {
    return "performanceReportList";
  }

  void setBuildAction(PerformanceBuildAction buildAction) {
    this.buildAction = buildAction;
  }

  public void setPerformanceReportMap(
      Map<String, PerformanceReport> performanceReportMap) {
    this.performanceReportMap = performanceReportMap;
  }

  public static String getPerformanceReportFileRelativePath(
      String reportFileName) {
    return getRelativePath(PERFORMANCE_REPORTS_DIRECTORY, reportFileName);
  }

  public static String getPerformanceReportDirRelativePath() {
    return getRelativePath(PERFORMANCE_REPORTS_DIRECTORY, null);
  }

  public static String getPerformanceSummaryDirRelativePath() {
    return getRelativePath(PERFORMANCE_SUMMARIES_DIRECTORY, null);
  }

  private static String getRelativePath(String dirname, String reportFileName) {
    StringBuilder sb = new StringBuilder(100);
    sb.append(dirname);
    if (reportFileName != null) {
      sb.append("/").append(reportFileName);
    }
    return sb.toString();
  }

  /**
   * <p>
   * Verify if the PerformanceReport exist the performanceReportName must to be like it
   * is in the build
   * </p>
   * 
   * @param performanceReportName
   * @return boolean
   */
  public boolean isFailed(String performanceReportName) {
    return getPerformanceReport(performanceReportName) == null;
  }
}
