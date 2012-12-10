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
	
	private DatabaseManager databaseManager()
	{
		if (!this.saved) {
			throw new RuntimeException("Knowledge base must be saved before connecting to its database.");
		}
		if (this.manager == null) {
			this.manager = new DatabaseManager(this.table());
		}
		return this.manager;
	}
	
	public Map<String, List<String>> targets()
	{
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		List<String> ageList = new LinkedList<String>();
		ageList.add("All");
		map.put("Age", ageList);
		
		List<String> genders = new LinkedList<String>();
		genders.add("All");
		map.put("Gender", genders);
		
		Ads targets = this.databaseManager().getTargets();
		
		if (targets == null || targets.numAttributes() == 0) {
			return map;
		}
		
		for (int i = 0; i < targets.attribute("Gender").numValues(); i++) {
			genders.add(targets.attribute("Gender").value(i));
		}

		Set<String> ages = new TreeSet<String>();
		Attribute attMinAge = targets.attribute("Age_Min");
		Attribute attMaxAge = targets.attribute("Age_Max");
		for (Instance instance : targets) {
			ages.add(instance.value(attMinAge) + "-" + instance.value(attMaxAge));
		}
		ageList.addAll(ages);
		
		return map;
	}
	
	public Ads getAds(Map<String, String> target)
	{
		this.databaseManager().resetWhere();
		for (String key : target.keySet()) {
			this.databaseManager().where(key, target.get(key));
		}
		Ads ads = this.databaseManager().get();
		return ads;
	}
	
	public Ads getAggregatedAds(Map<String, String> target, String aggregate)
	{
		this.databaseManager().resetWhere();
		for (String key : target.keySet()) {
			this.databaseManager().where(key, target.get(key));
		}
		Ads ads = this.databaseManager().getAggregate(aggregate);
		return ads;
	}
	
	public void addAds(Ads ads)
	{
		this.databaseManager().add(ads);
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
