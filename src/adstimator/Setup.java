package adstimator;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.experiment.DatabaseUtils;

/**
 * Setup is run to check if the application is started for the first time. If so, it needs to initialize the database so
 * that everything will function correctly.
 *
 * @author erikbrannstrom
 */
public class Setup
{

	public static boolean isFirstRun()
	{
		try {
			Connection con = DriverManager.getConnection(Setup.getDatabaseURL(), null, null);
			DatabaseMetaData meta = con.getMetaData();
			ResultSet rs = meta.getTables(null, null, "knowledge_bases", null);
			boolean first = !rs.next();
			con.close();
			return first;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void init() throws Exception
	{
		Connection con = DriverManager.getConnection(Setup.getDatabaseURL(), null, null);
		Statement stmnt = con.createStatement();
		
		// Create tables for KBs and config items
		String kbTable = "CREATE TABLE knowledge_bases (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(128))";
		String configTable = "CREATE TABLE config_items (key VARCHAR(32) PRIMARY KEY, value VARCHAR(128))";
		stmnt.execute(kbTable);
		stmnt.execute(configTable);
		
		// Insert default KB and set mark it as current in config
		PreparedStatement kbPreparedStatement = con.prepareStatement("INSERT INTO knowledge_bases VALUES (null, ?)");
		kbPreparedStatement.setString(1, "Default");
		kbPreparedStatement.execute();
		int rowId = kbPreparedStatement.getGeneratedKeys().getInt(1);
		
		PreparedStatement configPreparedStatement = con.prepareStatement("INSERT INTO config_items VALUES (?, ?)");
		configPreparedStatement.setString(1, "knowledge_base");
		configPreparedStatement.setInt(2, rowId);
		configPreparedStatement.execute();
		
		// Close connection
		stmnt.close();
		con.close();
	}
	
	private static String getDatabaseURL()
	{
		DatabaseUtils wekaDbUtil = null;
		try {
			wekaDbUtil = new DatabaseUtils();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		return wekaDbUtil.getDatabaseURL();
	}
}
