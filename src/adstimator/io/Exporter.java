package adstimator.io;

import java.io.*;
import java.util.*;

/**
 * Class for exporting values based on a template file.
 * 
 * The exporter needs to be initialized with a template file which is expected to have two lines. The first line
 * is the header which will be replicated in the output file as well. The second line is the line template, which will
 * be repeated as many times as required.
 * 
 * The template line can (and should, otherwise it would be useless) placeholders for values. A placeholder is the
 * name of the key wrapped in curly brackets, i.e. {key}. The key-values for each row are then input as a list of maps
 * to the export method.
 * 
 * @author erikbrannstrom
 */
public class Exporter
{
	private File templateFile;
	private String headerLine, rowTemplate;

	/**
	 * Initialize a new exporter with the specific template file.
	 * 
	 * @param templateFile 
	 */
	public Exporter(File templateFile)
	{
		this.templateFile = templateFile;
	}

	/**
	 * Initialize a new exporter with the template file as the specified path.
	 * 
	 * @param templateFile 
	 */
	public Exporter(String templateFile)
	{
		this(new File(templateFile));
	}

	/**
	 * Export values to a new file.
	 * 
	 * The file will be automatically created and the contents written. The values used to create the file should map
	 * to the keys used in the template file, with one map for each line in the output file. Any placeholders which
	 * were not replaced with a value will simply be removed from the output file.
	 * 
	 * @param outputFile
	 * @param values 
	 */
	public void export(File outputFile, List<Map<String,String>> values)
	{
		// Read template
		if (this.headerLine == null) {
			try {
				Scanner scanner = new Scanner(this.templateFile);
				this.headerLine = scanner.nextLine();
				this.rowTemplate = scanner.nextLine();
				scanner.close();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		// Loop through values and create new file
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write(this.headerLine);
			writer.newLine();
			for (Map<String,String> map : values) {
				String line = this.rowTemplate;
				// Replace placeholders with values from map
				for (String key : map.keySet()) {
					line = line.replaceAll(String.format("\\{%s\\}", key), map.get(key));
				}
				// Remove any unused placeholders
				line = line.replaceAll("\\{.+?\\}", "");
				// Write to file
				writer.write(line);
				writer.newLine();
				writer.flush();
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}