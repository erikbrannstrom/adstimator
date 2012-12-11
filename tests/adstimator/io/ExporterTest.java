package adstimator.io;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author erikbrannstrom
 */
public class ExporterTest
{

	/**
	 * Test of export method, of class Exporter.
	 */
	@Test
	public void testExport() throws Exception
	{
		Exporter instance = new Exporter("resources/export-template.csv");
		String expected = getFileContents(new File("resources/tests/export-expected.csv"));
		List<Map<String, String>> values = new LinkedList<Map<String, String>>();
		Map<String, String> line1 = new HashMap<String, String>();
		line1.put("Campaign Name", "Test Results");
		line1.put("Body", "Text");
		line1.put("Image Hash", "Image");
		line1.put("Gender", "Men");
		line1.put("Age Min", "18");
		line1.put("Age Max", "20");
		values.add(line1);
		Map<String, String> line2 = new HashMap<String, String>();
		line2.put("Campaign Name", "Test Results");
		line2.put("Body", "Another text");
		line2.put("Image Hash", "Image");
		line2.put("Gender", "Men");
		line2.put("Age Min", "21");
		line2.put("Age Max", "24");
		values.add(line2);
		
		// The list of maps contains the same values used to manually create the expected file, so they should match
		File outputFile = File.createTempFile("test", ".csv");
		instance.export(outputFile, values);
		String output = getFileContents(outputFile);
		outputFile.delete();
		assertEquals("Exported file contents does not match expected file contents.", expected, output);
		
		// By changing the value of the body for one line, the exported file should no longer match the expected one
		values.clear();
		values.add(line1);
		line2.put("Body", "Another another text");
		values.add(line2);
		outputFile = File.createTempFile("test", ".csv");
		instance.export(outputFile, values);
		output = getFileContents(outputFile);
		outputFile.delete();
		assertNotEquals("Exported file contents matched expected file contents.", expected, output);
	}

	/**
	 * Helper method for reading contents of file to string.
	 * 
	 * @param file file to read
	 * @return contents as string
	 */
	private static String getFileContents(File file) throws Exception
	{
		StringBuilder builder = new StringBuilder();
		Scanner scanner = new Scanner(file);
		while (scanner.hasNextLine()) {
			builder.append(scanner.nextLine());
		}
		return builder.toString();
	}
}
