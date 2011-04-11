package hudson.plugins.performance;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.IOException2;

import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Parser for JMeter.
 * 
 * @author Kohsuke Kawaguchi
 */
public class JMeterParser extends PerformanceReportParser {

  @Extension
  public static class DescriptorImpl extends PerformanceReportParserDescriptor {
    @Override
    public String getDisplayName() {
      return "JMeter";
    }
  }

  @DataBoundConstructor
  public JMeterParser(String glob) {
    super(glob);
  }

  @Override
  public String getDefaultGlobPattern() {
    return "**/*.jtl";
  }

  @Override
  public Collection<PerformanceReport> parse(AbstractBuild<?, ?> build,
      Collection<File> reports, TaskListener listener) throws IOException {
    List<PerformanceReport> result = new ArrayList<PerformanceReport>();

    for (File f : reports) {
      try {
        result.add(parse(build, f, listener));
      } catch (ParseException e) {
        // Don't add this report to the results
      }
    }
    return result;
  }

  @Override
  public PerformanceReport parse(AbstractBuild<?, ?> build,
      File report, TaskListener listener) throws IOException, ParseException {

    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(false);
    PrintStream logger = listener.getLogger();
    try {
      SAXParser parser = factory.newSAXParser();
      final PerformanceReport r = new PerformanceReport();
      r.setBuild(build);
      r.setReportFileName(report.getName());
      logger.println("Performance: Parsing JMeter report file " + report.getName());
      parser.parse(report, new DefaultHandler() {
        HttpSample currentSample;
        int counter = 0;

        /**
         * Performance XML log format is in
         * http://jakarta.apache.org
         * /jmeter/usermanual/listeners.html
         * 
         * There are two different tags which delimit jmeter
         * samples: httpSample for http samples sample for non http
         * samples
         * 
         * There are also two different XML formats which we have to
         * handle: v2.0 = "label", "timeStamp", "time", "success"
         * v2.1 = "lb", "ts", "t", "s"
         * 
         */
        @Override
        public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
          if ("httpSample".equalsIgnoreCase(qName)
              || "sample".equalsIgnoreCase(qName)) {
            HttpSample sample = new HttpSample();
            sample.setDate(new Date(
                Long.valueOf(attributes.getValue("ts") != null
                    ? attributes.getValue("ts")
                    : attributes.getValue("timeStamp"))));
            sample.setDuration(Long.valueOf(attributes.getValue("t") != null
                ? attributes.getValue("t") : attributes.getValue("time")));
            sample.setSuccessful(Boolean.valueOf(attributes.getValue("s") != null
                ? attributes.getValue("s") : attributes.getValue("success")));
            sample.setUri(attributes.getValue("lb") != null
                ? attributes.getValue("lb") : attributes.getValue("label"));
            if (counter == 0) {
              currentSample = sample;
            }
            counter++;
          }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
          if ("httpSample".equalsIgnoreCase(qName)
              || "sample".equalsIgnoreCase(qName)) {
            if (counter == 1) {
              try {
                r.addSample(currentSample);
              } catch (SAXException e) {
                e.printStackTrace();
              }
            }
            counter--;
          }
        }

      });
      return r;
    } catch (ParserConfigurationException e) {
      throw new IOException2("Failed to create parser ", e);
    } catch (SAXException e) {
      logger.println("Performance: Failed to parse " + report + ": "
          + e.getMessage());
      throw new ParseException(report, e.getMessage());
    }
  }
}
