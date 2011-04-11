package hudson.plugins.performance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import hudson.util.StreamTaskListener;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class PerformanceReportTest {

	private PerformanceReport performanceReport;

	@Before
	public void setUp() throws Exception {
		PrintStream printStream = EasyMock.createMock(PrintStream.class);
		performanceReport = new PerformanceReport();
		performanceReport.setErrorStream(printStream);
	}

	@Test
	public void testAddSample() throws Exception {

		HttpSample sample1 = new HttpSample();
		long duration = 23;
		sample1.setDuration(duration);
		performanceReport.addSample(sample1);

		sample1.setUri("invalidCharacter/");
		performanceReport.addSample(sample1);
		UriReport uriReport = performanceReport.getUriReportMap().get(
				"invalidCharacter_");
		assertNotNull(uriReport);

		String uri = "uri";
		sample1.setUri(uri);
		performanceReport.addSample(sample1);
		Map<String, ? extends UriReport> uriReportMap = performanceReport
				.getUriReportMap();
		uriReport = uriReportMap.get(uri);
		assertNotNull(uriReport);
		assertEquals(1, uriReport.size());
		assertEquals(2, performanceReport.size());
		assertEquals(duration, uriReport.getMax());
		assertEquals(duration, uriReport.getMax());
		assertEquals(duration, performanceReport.getMin());
		assertEquals(duration, performanceReport.getMin());
	}

	@Test
	public void testCountError() throws SAXException {
		HttpSample sample1 = new HttpSample();
		sample1.setSuccessful(false);
		sample1.setUri("sample1");
		performanceReport.addSample(sample1);

		HttpSample sample2 = new HttpSample();
		sample2.setSuccessful(true);
		sample2.setUri("sample2");
		performanceReport.addSample(sample2);
		assertEquals(1, performanceReport.countErrors());
	}

	@Test
	public void testPerformanceReport() throws IOException, SAXException {
		PerformanceReport performanceReport = parseOneJMeter(new File(
				"src/test/resources/JMeterResults.jtl"));
		Map<String, UriReport> uriReportMap = performanceReport
				.getUriReportMap();
		assertEquals(2, uriReportMap.size());
		String loginUri = "Home";
		UriReport firstUriReport = uriReportMap.get(loginUri);
		assertEquals(501, firstUriReport.getMin());
		assertEquals(15902, firstUriReport.getMax());
		assertEquals(0, firstUriReport.countErrors());
		String logoutUri = "Workgroup";
		UriReport secondUriReport = uriReportMap.get(logoutUri);
		assertEquals(58, secondUriReport.getMin());
		assertEquals(1017, secondUriReport.getMax());
		assertEquals(0, secondUriReport.countErrors());
	}

	private PerformanceReport parseOneJMeter(File f) throws IOException {
		return new JMeterParser("").parse(null, Collections.singleton(f),
				new StreamTaskListener(System.out)).iterator().next();
	}

	private PerformanceReport parseOneJUnit(File f) throws IOException {
		return new JUnitParser("").parse(null, Collections.singleton(f),
				new StreamTaskListener(System.out)).iterator().next();
	}

	@Test
	public void testPerformanceNonHTTPSamplesMultiThread() throws IOException,
			SAXException {
		PerformanceReport performanceReport = parseOneJMeter(new File(
				"src/test/resources/JMeterResultsMultiThread.jtl"));

		Map<String, UriReport> uriReportMap = performanceReport
				.getUriReportMap();
		assertEquals(1, uriReportMap.size());

		String uri = "WebService(SOAP) Request";
		UriReport report = uriReportMap.get(uri);
		assertNotNull(report);

		assertEquals(894, report.getMin());
		assertEquals(1581, report.getMax());
		assertEquals(1272, report.getAverage());
	}

	@Test
	public void testPerformanceReportJUnit() throws IOException, SAXException {
		PerformanceReport performanceReport = parseOneJUnit(new File(
				"src/test/resources/TEST-JUnitResults.xml"));
		Map<String, UriReport> uriReportMap = performanceReport
				.getUriReportMap();
		assertEquals(5, uriReportMap.size());
		String firstUri = "testGetMin";
		UriReport firstUriReport = uriReportMap.get(firstUri);
		assertEquals(31, firstUriReport.getMax());
		assertEquals(31, firstUriReport.getMin());
		assertEquals(0, firstUriReport.countErrors());
		String lastUri = "testGetMax";
		UriReport secondUriReport = uriReportMap.get(lastUri);
		assertEquals(26, secondUriReport.getMax());
		assertEquals(26, secondUriReport.getMin());
		assertEquals(1, secondUriReport.countErrors());
	}
		

		@Test
	public void testPerformanceReportMultiLevel() throws IOException, SAXException {
		PerformanceReport performanceReport = parseOneJMeter(new File(
				"src/test/resources/JMeterResultsMultiLevel.jtl"));
		Map<String, UriReport> uriReportMap = performanceReport
				.getUriReportMap();
		assertEquals(2, uriReportMap.size());
		UriReport report = uriReportMap.get("Home");
		assertNotNull(report);
	}
}
