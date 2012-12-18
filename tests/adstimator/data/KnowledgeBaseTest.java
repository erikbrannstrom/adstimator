package adstimator.data;

import adstimator.Setup;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.converters.ConverterUtils;

/**
 *
 * @author erikbrannstrom
 */
public class KnowledgeBaseTest
{
	
	public KnowledgeBaseTest()
	{
	}
	
	@BeforeClass
	public static void setUpClass() throws ClassNotFoundException
	{
		Class.forName("org.sqlite.JDBC");
		DatabaseHelper.instance().setConnectionURL("jdbc:sqlite:resources/tests/test.db");
		if (Setup.isFirstRun()) {
			Setup.init();
		}
	}
	
	@AfterClass
	public static void tearDownClass()
	{
		File db = new File("resources/tests/test.db");
		if (db.exists()) {
			db.delete();
		}
	}
	
	@Before
	public void setUp()
	{
	}
	
	@After
	public void tearDown()
	{
		// Empty the KB table after each test
		try {
			Connection con = DatabaseHelper.instance().getConnection();
			Statement delete = con.createStatement();
			delete.execute("DELETE FROM knowledge_bases");
			delete.close();
			con.close();
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Test of id method, of class KnowledgeBase.
	 */
	@Test
	public void testId()
	{
		KnowledgeBase kb = new KnowledgeBase("Test");
		assertEquals(0, kb.id());
		kb.save();
		assertTrue(kb.id() > 0);
	}

	/**
	 * Test of name method, of class KnowledgeBase.
	 */
	@Test
	public void testName_0args()
	{
		KnowledgeBase instance = new KnowledgeBase("Test");
		assertEquals("Test", instance.name());
		instance.save();
		assertEquals("Test", instance.name());
	}

	/**
	 * Test of name method, of class KnowledgeBase.
	 */
	@Test
	public void testName_String()
	{
		KnowledgeBase instance = new KnowledgeBase("Test");
		assertEquals(instance.name(), "Test");
		instance.name("Test2");
		assertEquals("Test2", instance.name());
	}

	/**
	 * Test of table method, of class KnowledgeBase.
	 */
	@Test
	public void testTable()
	{
		KnowledgeBase instance = new KnowledgeBase("Test");
		instance.save();
		String expected = "kb" + instance.id();
		assertEquals(expected, instance.table());
	}

	/**
	 * Test of targets method, of class KnowledgeBase.
	 */
	@Test
	public void testTargets() throws Exception
	{
		KnowledgeBase instance = new KnowledgeBase("Test");
		instance.save();
		ConverterUtils.DataSource source = new ConverterUtils.DataSource("resources/tests/AdsTest.csv");
		Ads ads = new Ads(source.getDataSet());
		instance.addAds(ads);
		Map<String, List<String>> targets = instance.targets();
		System.out.println(targets);
		// Assert that gender contains both men and women, as well as All
		List<String> genders = targets.get("Gender");
		assertNotNull(genders);
		assertTrue("Men was not found.", genders.contains("Men"));
		assertTrue("Women was not found.", genders.contains("Women"));
		assertTrue("All was not found.", genders.contains("All"));
		// Assert that age groups contain both 18-23 and 24-28, as well as All
		List<String> ages = targets.get("Age");
		assertNotNull(ages);
		assertTrue("18-23 was not found.", ages.contains("18-23"));
		assertTrue("24-28 was not found.", ages.contains("24-28"));
		assertTrue("All was not found.", ages.contains("All"));
	}

	/**
	 * Test of getAds method, of class KnowledgeBase.
	 */
	@Test
	public void testGetAds() throws Exception
	{
		KnowledgeBase instance = new KnowledgeBase("Test");
		instance.save();
		ConverterUtils.DataSource source = new ConverterUtils.DataSource("resources/tests/AdsTest.csv");
		Ads ads = new Ads(source.getDataSet());
		instance.addAds(ads);
		// Get only ads targeting men ages 18-23
		Map<String, String> target = new HashMap<String, String>();
		target.put("Gender", "Men");
		target.put("Age", "18-23");
		Ads result = instance.getAds(target);
		assertNotNull(result);
		assertTrue(result.size() == 1);
		// All ads from one set should be in the other and vice versa, only without the target
		result = instance.getAds(null);
		ads.deleteAttributeAt(ads.attribute("Gender").index());
		ads.deleteAttributeAt(ads.attribute("Age Min").index());
		ads.deleteAttributeAt(ads.attribute("Age Max").index());
		Set<String> adStrings = new HashSet<String>();
		for (Instance ad : ads) {
			adStrings.add(ad.toString());
		}
		for (Instance ad : result) {
			assertTrue(adStrings.remove(ad.toString()));
		}
		assertTrue(adStrings.isEmpty());
	}

	/**
	 * Test of getAggregatedAds method, of class KnowledgeBase.
	 */
	@Test
	public void testGetAggregatedAds() throws Exception
	{
		KnowledgeBase instance = new KnowledgeBase("Test");
		instance.save();
		ConverterUtils.DataSource source = new ConverterUtils.DataSource("resources/tests/AdsTest.csv");
		Ads ads = new Ads(source.getDataSet());
		instance.addAds(ads);
		
		// Aggregate on image for all targets
		Map<String, Double> expected = new HashMap<String, Double>();
		expected.put("Image-1", 0.05);
		expected.put("Image-2", 0.056);
		expected.put("Image-3", 0.045);
		Ads result = instance.getAggregatedAds(null, "Image Hash");
		assertNotNull(result);
		Attribute imageAttr = result.attribute("Image Hash");
		Attribute clicksAttr = result.attribute("Clicks Count");
		Attribute impressionsAttr = result.attribute("Impressions");
		for (Instance aggregate : result) {
			double clicks = aggregate.value(clicksAttr);
			double impressions = aggregate.value(impressionsAttr);
			assertEquals(expected.get(aggregate.stringValue(imageAttr)), (clicks/impressions), 0.00001);
		}
		
		// Aggregate on text for ads targeting men
		Map<String, String> target = new HashMap<String, String>();
		target.put("Gender", "Men");
		result = instance.getAggregatedAds(target, "Body");
		assertNotNull(result);
		assertTrue(result.size() == 1);
		clicksAttr = result.attribute("Clicks Count");
		impressionsAttr = result.attribute("Impressions");
		Instance ad = result.firstInstance();
		assertEquals(15, ad.value(clicksAttr), 0.00001);
		assertEquals(350, ad.value(impressionsAttr), 0.00001);
	}

	/**
	 * Test of addAds method, of class KnowledgeBase.
	 */
	@Test
	public void testAddAds() throws Exception
	{
		KnowledgeBase instance = new KnowledgeBase("Test");
		instance.save();
		Ads ads = instance.getAds(null);
		assertNull(ads);
		// Add some ads
		ConverterUtils.DataSource source = new ConverterUtils.DataSource("resources/tests/AdsTest.csv");
		ads = new Ads(source.getDataSet());
		instance.addAds(ads);
		ads = instance.getAds(null);
		assertNotNull(ads);
		assertTrue(ads.size() == 4);
	}

	/**
	 * Test of getAll method, of class KnowledgeBase.
	 */
	@Test
	public void testGetAll()
	{
		assertTrue(KnowledgeBase.getAll().isEmpty());
		KnowledgeBase instance = new KnowledgeBase("Test");
		assertTrue(KnowledgeBase.getAll().isEmpty());
		instance.save();
		assertTrue(KnowledgeBase.getAll().size() == 1);
		for (int i = 0; i < 3; i++) {
			instance = new KnowledgeBase("Test " + i);
			instance.save();
		}
		assertTrue(KnowledgeBase.getAll().size() == 4);
	}

	/**
	 * Test of find method, of class KnowledgeBase.
	 */
	@Test
	public void testFind()
	{
		KnowledgeBase instance = new KnowledgeBase("Test");
		instance.save();
		KnowledgeBase result = KnowledgeBase.find(instance.id());
		assertEquals(instance, result);
		assertNull(KnowledgeBase.find(instance.id()+1));
	}

	/**
	 * Test of save method, of class KnowledgeBase.
	 */
	@Test
	public void testSave()
	{
		KnowledgeBase instance = new KnowledgeBase("Test");
		assertTrue(instance.id() == 0);
		instance.save();
		int id = instance.id();
		assertTrue(id > 0);
		assertTrue(KnowledgeBase.find(id).name().equals("Test"));
	}

	/**
	 * Test of delete method, of class KnowledgeBase.
	 */
	@Test
	public void testDelete()
	{
		KnowledgeBase instance = new KnowledgeBase("Test");
		assertTrue(instance.id() == 0);
		instance.save();
		int id = instance.id();
		assertNotNull(KnowledgeBase.find(id));
		instance.delete();
		assertNull(KnowledgeBase.find(id));
	}

}
