package adstimator.data;

import adstimator.utils.DatabaseHelper;
import java.sql.Connection;
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

/**
 * Data access object for the knowledge base.
 * 
 * A knowledge base has an ID and a name as well as a set of instances.
 *
 * @author erikbrannstrom
 */
public class KnowledgeBase
{
	private boolean exists, saved;
	private int id;
	private String name;
	private DatabaseManager manager;
	
	/**
	 * Get ID of this knowledge base. If it has not been saved, the ID is zero.
	 * 
	 * @return id for KB
	 */
	public int id()
	{
		return this.id;
	}

	/**
	 * Get name of knowledge base.
	 * 
	 * @return name
	 */
	public String name()
	{
		return name;
	}
	
	/**
	 * Set the name of this knowledge base. This also marks the object as in need of saving.
	 * 
	 * @param name New name
	 */
	public void name(String name)
	{
		this.name = name;
		this.saved(false);
	}
	
	/**
	 * Returns the table name where the ads belonging to the knowledge base are stored.
	 * 
	 * @return 
	 */
	public String table()
	{
		return "kb" + this.id();
	}

	/**
	 * Create a new knowledge base with a specific ID and name. ID is expected to match the primary key of the table
	 * storing the knowledge bases.
	 * 
	 * @param id 
	 * @param name 
	 */
	public KnowledgeBase(int id, String name)
	{
		this.id = id;
		this.name = name;
	}
	
	/**
	 * Create a new knowledge base with a specific name. Same as calling new KnowledgeBase(0, name).
	 * 
	 * @param name 
	 */
	public KnowledgeBase(String name)
	{
		this(0, name);
	}
	
	/**
	 * Set whether or not this knowledge base is stored in the database.
	 * 
	 * @param exists 
	 */
	private void exists(boolean exists)
	{
		this.exists = exists;
	}
	
	/**
	 * Set whether or not the current state of the object has been saved to the database.
	 * 
	 * @param saved 
	 */
	private void saved(boolean saved)
	{
		this.saved = saved;
		if (saved) {
			this.exists(true);
		}
	}
	
	/**
	 * Private access method for the database manager.
	 * 
	 * @return Database access manager
	 */
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
	
	/**
	 * Get a map where the keys are the targeting properties used in the data set and the values are lists of the
	 * values that each property can take.
	 * 
	 * @return Target properties and their values
	 */
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
		Attribute attMinAge = targets.attribute("Age Min");
		Attribute attMaxAge = targets.attribute("Age Max");
		for (Instance instance : targets) {
			ages.add((int)instance.value(attMinAge) + "-" + (int)instance.value(attMaxAge));
		}
		ageList.addAll(ages);
		
		return map;
	}
	
	/**
	 * Get all ads that matches the specified target.
	 * 
	 * @param target targeting, property => value, and null means all targets
	 * @return 
	 */
	public Ads getAds(Map<String, String> target)
	{
		this.databaseManager().resetWhere();
		if (target != null) {
			for (String key : target.keySet()) {
				this.databaseManager().where(key, target.get(key));
			}
		}
		Ads ads = this.databaseManager().get();
		return ads;
	}
	
	/**
	 * Get the aggregate of all ads matching the target with aggregation on the specified attribute.
	 * 
	 * @param target
	 * @param aggregate
	 * @return 
	 */
	public Ads getAggregatedAds(Map<String, String> target, String aggregate)
	{
		this.databaseManager().resetWhere();
		if (target != null) {
			for (String key : target.keySet()) {
				this.databaseManager().where(key, target.get(key));
			}
		}
		Ads ads = this.databaseManager().getAggregate(aggregate);
		return ads;
	}
	
	/**
	 * Store the ads provided in this knowledge base by appending them to the knowledge base table.
	 * 
	 * @param ads Ads to be stored
	 */
	public void addAds(Ads ads)
	{
		this.databaseManager().add(ads);
	}

	/**
	 * Get all knowledge bases.
	 * 
	 * @return List of knowledge bases
	 */
	public static List<KnowledgeBase> getAll()
	{
		try {
			Connection con = DatabaseHelper.instance().getConnection();
			PreparedStatement readAll = con.prepareStatement("SELECT id, name FROM knowledge_bases");
			ResultSet rs = readAll.executeQuery();
			List<KnowledgeBase> list = new LinkedList<KnowledgeBase>();
			while (rs.next()) {
				KnowledgeBase kb = new KnowledgeBase(rs.getInt("id"), rs.getString("name"));
				kb.saved(true);
				list.add(kb);
			}
			readAll.close();
			con.close();
			return list;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Get a single knowledge base based on its ID.
	 * 
	 * @param id Knowledge base ID
	 * @return 
	 */
	public static KnowledgeBase find(int id)
	{
		try {
			Connection con = DatabaseHelper.instance().getConnection();
			PreparedStatement find = con.prepareStatement("SELECT id, name FROM knowledge_bases WHERE id = ?");
			find.setInt(1, id);
			ResultSet rs = find.executeQuery();
			if (rs.isAfterLast()) {
				// Query didn't return any matches
				return null;
			}
			KnowledgeBase kb = new KnowledgeBase(rs.getInt("id"), rs.getString("name"));
			kb.saved(true);
			find.close();
			con.close();
			return kb;
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Save the object, either inserting it to the database if it does not already exist or otherwise updating 
	 * its record.
	 */
	public void save()
	{
		try {
			Connection con = DatabaseHelper.instance().getConnection();
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
	
	/**
	 * Remove this object from the database. The object itself is not however reset and can be saved again.
	 */
	public void delete()
	{
		if (!this.exists) {
			return;
		}
		
		try {
			Connection con = DatabaseHelper.instance().getConnection();
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

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof KnowledgeBase)) {
			return false;
		}
		KnowledgeBase kb = (KnowledgeBase)o;
		return kb.id() == this.id() && this.name().equals(kb.name());
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 29 * hash + this.id;
		hash = 29 * hash + (this.name != null ? this.name.hashCode() : 0);
		return hash;
	}
	
}
