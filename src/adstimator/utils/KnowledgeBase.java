/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package adstimator.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.experiment.DatabaseUtils;

/**
 *
 * @author erikbrannstrom
 */
public class KnowledgeBase
{
	private boolean exists, saved;
	private int id;
	private String name;
	
	public int id()
	{
		return this.id;
	}

	public String name()
	{
		return name;
	}
	
	public void name(String name)
	{
		this.name = name;
		this.saved(false);
	}

	public KnowledgeBase(int id, String name)
	{
		this.id = id;
		this.name = name;
	}
	
	public KnowledgeBase(String name)
	{
		this(0, name);
	}
	
	protected void exists(boolean exists)
	{
		this.exists = exists;
	}
	
	protected void saved(boolean saved)
	{
		this.saved = saved;
		if (saved) {
			this.exists(true);
		}
	}

	public static List<KnowledgeBase> getAll()
	{
		try {
			Connection con = getConnection();
			PreparedStatement readAll = con.prepareStatement("SELECT id, name FROM knowledge_bases");
			ResultSet rs = readAll.executeQuery();
			List<KnowledgeBase> list = new LinkedList<KnowledgeBase>();
			while (rs.next()) {
				KnowledgeBase kb = new KnowledgeBase(rs.getInt("id"), rs.getString("name"));
				kb.saved(true);
				list.add(kb);
			}
			con.close();
			return list;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public void save()
	{
		try {
			Connection con = KnowledgeBase.getConnection();
			if (this.exists && !this.saved) {
				PreparedStatement update = con.prepareStatement("UPDATE knowledge_bases SET name = ? WHERE id = ?");
				update.setString(1, this.name);
				update.setInt(2, this.id);
				update.executeUpdate();
				update.close();
			} else if (!this.exists) {
				PreparedStatement insert = con.prepareStatement("INSERT INTO knowledge_bases VALUES (null, ?)");
				insert.setString(1, this.name);
				insert.execute();
				this.id = insert.getGeneratedKeys().getInt(1);
				insert.close();
			}
			this.saved(true);
			con.close();
		} catch (SQLException ex) {
			
		}
	}
	
	/**
	 * Helper method for getting a connection to the database specified by Weka.
	 * 
	 * @return Weka database connection
	 */
	public static Connection getConnection()
	{
		try {
			DatabaseUtils wekaDbUtil = new DatabaseUtils();
			return DriverManager.getConnection(wekaDbUtil.getDatabaseURL(), null, null);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
}
