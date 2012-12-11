package adstimator.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.experiment.DatabaseUtils;

/**
 *
 * @author erikbrannstrom
 */
public class DatabaseHelper
{

	private static DatabaseHelper instance;
	private String connectionURL;

	protected DatabaseHelper()
	{
	}

	public String getConnectionURL()
	{
		if (this.connectionURL == null) {
			try {
				DatabaseUtils wekaDbUtil = new DatabaseUtils();
				this.connectionURL = wekaDbUtil.getDatabaseURL();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return this.connectionURL;
	}

	public void setConnectionURL(String connectionURL)
	{
		this.connectionURL = connectionURL;
	}
	
	public Connection getConnection()
	{
		try {
			return DriverManager.getConnection(this.getConnectionURL(), null, null);
		} catch (SQLException ex) {
			return null;
		}
	}

	public static DatabaseHelper instance()
	{
		if (instance == null) {
			instance = new DatabaseHelper();
		}
		return instance;
	}
}
