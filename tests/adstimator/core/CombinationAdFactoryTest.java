package adstimator.core;

import adstimator.data.Ads;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.converters.ConverterUtils;

/**
 *
 * @author erikbrannstrom
 */
public class CombinationAdFactoryTest
{
	private Ads ads;

	@Before
	public void setUp()
	{
		try {
			ConverterUtils.DataSource source = new ConverterUtils.DataSource("resources/tests/AdsTest.csv");
			this.ads = new Ads(source.getDataSet());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Test of all method, of class CombinationAdFactory.
	 */
	@Test
	public void testAll()
	{
		CombinationAdFactory factory = new CombinationAdFactory(this.ads);
		// Two suggested ads should be returned
		Ads result = factory.all();
		assertTrue("Less than two ads were returned.", result.size() == 2);
		// By adding the [Text-2, Image-1] and [Text-2, Image-3] combos, no ads should be returned
		Instance inst = this.ads.firstInstance();
		Instance combo1 = new DenseInstance(inst);
		combo1.setDataset(this.ads);
		combo1.setValue(3, "Text-2");
		combo1.setValue(4, "Image-1");
		Instance combo2 = new DenseInstance(inst);
		combo2.setDataset(this.ads);
		combo2.setValue(3, "Text-2");
		combo2.setValue(4, "Image-3");
		this.ads.add(combo1);
		this.ads.add(combo2);
		factory = new CombinationAdFactory(this.ads);
		result = factory.all();
		assertTrue("Ads were returned.", result.isEmpty());
	}
}
