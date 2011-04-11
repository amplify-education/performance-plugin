package hudson.plugins.performance;

import static org.junit.Assert.*;

import hudson.plugins.performance.HttpSample;
import hudson.plugins.performance.UriReport;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class UriReportTest {

	private UriReport uriReport;

	@Before
	public void setUp() {
		uriReport = new UriReport(null, null, null);
		Date date = new Date();
		for (int i = 0; i < 11; i++) {
			HttpSample httpSample = new HttpSample();
			httpSample.setDuration(i);
			httpSample.setDate(date);
			httpSample.setSuccessful(i%2 == 0);
			uriReport.addHttpSample(httpSample);
		}
	}

	@Test
	public void testCountErrors() {
		assertEquals(5, uriReport.countErrors());
	}

	@Test
	public void testGetAverage() {
		assertEquals(5, uriReport.getAverage());
	}

	@Test
	public void testGetMax() {
		assertEquals(10, uriReport.getMax());
	}

	@Test
	public void testGetMin() {
		assertEquals(0, uriReport.getMin());
	}

	@Test
	public void testGetMedian() {
		assertEquals(5, uriReport.getMedian());
	}

	@Test
	public void testGet90Line() {
		assertEquals(9, uriReport.get90Line());
	}

	@Test
	public void testIsFailed() {
		assertTrue(uriReport.isFailed());
	}

}
