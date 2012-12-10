/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package adstimator.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import weka.core.Attribute;
import weka.core.Instance;
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
	private DatabaseManager manager;
	
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
	
	public String table()
	{
		return "kb" + this.id();
	}

	public KnowledgeBase(int id, String name)
	{
		this.id = id;
		this.name = name;
		this.manager = new DatabaseManager(this.table());
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
	
	public Map<String, List<String>> targets()
	{
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		Ads targets = this.manager.getTargets();
		
		List<String> genders = new LinkedList<String>();
		for (int i = 0; i < targets.attribute("Gender").numValues(); i++) {
			genders.add(targets.attribute("Gender").value(i));
		}
		genders.add("All");
		map.put("Gender", genders);

		Set<String> ages = new TreeSet<String>();
		Attribute attMinAge = targets.attribute("Age_Min");
		Attribute attMaxAge = targets.attribute("Age_Max");
		for (Instance instance : targets) {
			ages.add(instance.value(attMinAge) + "-" + instance.value(attMaxAge));
		}
		List<String> ageList = new LinkedList<String>(ages);
		ageList.add("All");
		map.put("Age", ageList);
		
		return map;
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
	
	public static KnowledgeBase find(int id)
	{
		try {
			Connection con = getConnection();
			PreparedStatement find = con.prepareStatement("SELECT id, name FROM knowledge_bases WHERE id = ?");
			find.setInt(1, id);
			ResultSet rs = find.executeQuery();
			KnowledgeBase kb = new KnowledgeBase(rs.getInt("id"), rs.getString("name"));
			kb.saved(true);
			con.close();
			return kb;
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
			throw new RuntimeException(ex);
		}
	}
	
	public void delete()
	{
		if (!this.exists) {
			return;
		}
		
		try {
			Connection con = KnowledgeBase.getConnection();
			PreparedStatement delete = con.prepareStatement("DELETE FROM knowledge_bases WHERE id = ?");
			delete.setInt(1, this.id());
			delete.executeUpdate();
			delete.close();
			Statement drop = con.createStatement();
			drop.execute(String.format("DROP TABLE IF EXISTS %s", this.table()));
			drop.close();
			con.close();
			this.saved(false);
			this.exists(false);
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
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
