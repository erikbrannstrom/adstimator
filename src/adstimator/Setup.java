package adstimator;

import adstimator.data.DatabaseHelper;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Setup is run to check if the application is started for the first time. If so, it needs to initialize the database so
 * that everything will function correctly.
 *
 * @author erikbrannstrom
 */
public class Setup
{

	/**
	 * Returns whether or not the application is properly set up. It does so by getting a database connection and 
	 * looking for the table knowledge_bases.
	 * 
	 * @return True if application is properly set up
	 */
	public static boolean isFirstRun()
	{
		try {
			Connection con = DatabaseHelper.instance().getConnection();
			DatabaseMetaData meta = con.getMetaData();
			ResultSet rs = meta.getTables(null, null, "knowledge_bases", null);
			boolean first = !rs.next();
			con.close();
			return first;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Create tables for knowledge bases and configuration.
	 */
	public static void init()
	{
		try {
			Connection con = DatabaseHelper.instance().getConnection();
			Statement stmnt = con.createStatement();
			
			// Create tables for KBs and config items
			String kbTable = "CREATE TABLE knowledge_bases (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(128))";
			String configTable = "CREATE TABLE config_items (key VARCHAR(32) PRIMARY KEY, value VARCHAR(128))";
			stmnt.execute(kbTable);
			stmnt.execute(configTable);
			
			// Insert default KB
			PreparedStatement kbPreparedStatement = con.prepareStatement("INSERT INTO knowledge_bases VALUES (null, ?)");
			kbPreparedStatement.setString(1, "Default");
			kbPreparedStatement.execute();
			int rowId = kbPreparedStatement.getGeneratedKeys().getInt(1);
			
			// Set default knowledge base in config
			PreparedStatement configPreparedStatement = con.prepareStatement("INSERT INTO config_items VALUES (?, ?)");
			configPreparedStatement.setString(1, "knowledge_base");
			configPreparedStatement.setInt(2, rowId);
			configPreparedStatement.execute();
			
			// Close connection
			stmnt.close();
			con.close();
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
