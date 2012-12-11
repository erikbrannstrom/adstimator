/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package adstimator.io;

import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author erikbrannstrom
 */
public class FacebookDataParserTest
{

	/**
	 * Test of parse method, of class FacebookDataParser.
	 */
	@Test
	public void testParse()
	{
		// The test report is an actual report from Facebook which has been modified slightly to be easier to
		// read and to anonymize some data points.
		File file = new File("resources/tests/facebook-report.csv");
		Instances results = FacebookDataParser.parse(file);
		
		// The report contains two ad instances
		assertTrue("Instances did not contain two ads.", results.size() == 2);
		
		// Check that the returned instances look as expected. For convenience, the string output is compared
		Instance first = results.firstInstance();
		Instance last = results.lastInstance();
		assertEquals("'Text, text, text',Image1,Women,32,34,50,10000", first.toString());
		assertEquals("'More text, more text!',Image2,Men,29,31,80,20000", last.toString());
				
		// Make sure the single quotes are only shown to not confuse commas in text with column separators
		assertEquals("Text, text, text", first.stringValue(0));
		assertEquals("More text, more text!", last.stringValue(0));
	}
}
