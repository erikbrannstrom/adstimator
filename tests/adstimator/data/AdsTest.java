package adstimator.data;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.converters.ConverterUtils.DataSource;

/**
 *
 * @author erikbrannstrom
 */
public class AdsTest
{
	private Ads ads;

	@Before
	public void setUp()
	{
		try {
			DataSource source = new DataSource("resources/tests/AdsTest.csv");
			this.ads = new Ads(source.getDataSet());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Test of findMatch method, of class Ads.
	 */
	@Test
	public void testFindMatch()
	{
		// Make a copy of the first instance, which is [F,18,23,Text-1,Image-1,5,100]
		Instance search = new DenseInstance(this.ads.firstInstance());
		// Change text to Text-2
		search.setDataset(this.ads);
		search.setValue(3, "Text-2");
		search.setValue(4, "Image-2");
		// This should match an instance from the data set
		Instance result = this.ads.findMatch(search);
		assertNotNull("No match was found for instance.", result);
		// Even with unique target we should find a matching instance
		search.setValue(1, 24.0);
		search.setValue(2, 29.0);
		result = this.ads.findMatch(search);
		assertNotNull("No match was found for instance with unique target group.", result);
		// With unique ad properties, no match should be found
		search.setValue(4, "Image-3");
		result = this.ads.findMatch(search);
		assertNull("Match was found for unique instance.", result);
	}

	/**
	 * Test of convertToRate method, of class Ads.
	 */
	@Test
	public void testConvertToRate()
	{
		this.ads.convertToRate();
		// First instance has 5 clicks and 100 impressions = 0.05 click rate
		Instance first = this.ads.firstInstance();
		assertEquals("Click rate was not correct.", 0.05, first.classValue(), 0.000001);
		// Last instance has 6 clicks and 150 impressions = 0.04 click rate
		Instance last = this.ads.lastInstance();
		assertEquals("Click rate was not correct.", 0.04, last.classValue(), 0.000001);
	}

	/**
	 * Test of averageActionRate method, of class Ads.
	 */
	@Test
	public void testAverageActionRate()
	{
		// Data set has total of 28 clicks and 550 impressions = 0.050909091 click rate
		double expResult = 0.050909091;
		double result = this.ads.averageActionRate();
		assertEquals("Average click rate was incorrect.", expResult, result, 0.000001);
	}
}
