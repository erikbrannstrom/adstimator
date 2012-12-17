package adstimator.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import weka.experiment.DatabaseUtils;

/**
 * Config class which uses database as its persistent storage.
 * 
 * The class loads all config items from the database upon initialization and stores them in a map for fast access.
 * When setting or removing an item, the action is immediately reproduced on the persistent storage.
 * 
 * The database used is the same one that is defined in the Weka property file (DatabaseUtils.props). The table
 * used is called 'config_items' and is automatically created by the Setup.init() method on the applications first
 * run.
 *
 * @author erikbrannstrom
 */
public class Config
{
	private Map<String, String> config;

	/**
	 * Create a new configuration object.
	 */
	public Config()
	{
		this.config = new HashMap<String, String>();
		try {
			Connection con = this.getConnection();
			PreparedStatement readAll = con.prepareStatement("SELECT key, value FROM config_items");
			ResultSet rs = readAll.executeQuery();
			while (rs.next()) {
				this.config.put(rs.getString("key"), rs.getString("value"));
			}
			con.close();
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Get the value of the specified config item.
	 * 
	 * @param key
	 * @return Value for key
	 */
	public String get(String key)
	{
		return this.config.get(key);
	}
	
	/**
	 * Set the value of the specified config item to the given value.
	 * 
	 * @param key
	 * @param value
	 */
	public void set(String key, String value)
	{
		// If setting key to same value as before we don't have to do anything
		if (this.config.get(key).equals(value)) {
			return;
		}
		
		try {
			Connection con = this.getConnection();
			
			if (this.config.containsKey(key)) {
				// Update
				PreparedStatement update = con.prepareStatement("UPDATE config_items SET value = ? WHERE key = ?");
				update.setString(1, value);
				update.setString(2, key);
				update.execute();
				update.close();
			} else {
				// Insert
				PreparedStatement create = con.prepareStatement("INSERT (key, value) INTO config_items VALUES (?, ?)");
				create.setString(1, key);
				create.setString(2, value);
				create.execute();
				create.close();
			}
			
			con.close();
			this.config.put(key, value);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Remove the specified config item.
	 * 
	 * @param key
	 */
	public void remove(String key)
	{
		try {
			Connection con = this.getConnection();
			PreparedStatement delete = con.prepareStatement("DELETE FROM config_items WHERE key = ?");
			delete.setString(1, key);
			delete.execute();
			delete.close();
			con.close();
			this.config.remove(key);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Private helper method for getting a connection to the database specified by Weka.
	 * 
	 * @return Weka database connection
	 */
	private Connection getConnection()
	{
		try {
			DatabaseUtils wekaDbUtil = new DatabaseUtils();
			return DriverManager.getConnection(wekaDbUtil.getDatabaseURL(), null, null);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
