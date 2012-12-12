package adstimator.data;

import java.util.*;
import weka.core.Attribute;
import weka.core.converters.DatabaseLoader;
import weka.core.converters.DatabaseSaver;

/**
 * Class for interacting with the database defined in the Weka database property file (DatabaseUtils.props).
 * 
 * Implements the DataManager interface, but also adds methods for getting aggregate data and targeting used in the
 * data set.
 * 
 * @author erikbrannstrom
 */
public class DatabaseManager implements DataManager
{
	private String tableName;
	private List<String> where;

	/**
	 * Constructor for initializing a database manager with a given table name.
	 * 
	 * @param tableName Name of table
	 */
	public DatabaseManager(String tableName)
	{
		this.tableName = tableName;
		this.where = new LinkedList<String>();
	}

	@Override
	public void add(Ads ads)
	{
		try {
			DatabaseSaver save = new DatabaseSaver();
			save.setInstances(ads);
			save.setRelationForTableName(false);
			save.setTableName(this.tableName);
			save.connectToDatabase();
			save.writeBatch();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Ads get()
	{
		return this.get("Body, Image_Hash, Clicks_Count, Impressions", "");
	}

	/**
	 * Private helper method for generating and executing SELECT queries.
	 * 
	 * @param select DB fields to select
	 * @param groupBy Field to group on
	 * @return
	 */
	private Ads get(String select, String groupBy)
	{
		String whereClause = this.createWhereClause();
		
		if (groupBy != null && groupBy.length() > 0) {
			groupBy = "GROUP BY " + groupBy;
		} else {
			groupBy = "";
		}
		
		try {
			DatabaseLoader loader = new DatabaseLoader();
			loader.connectToDatabase();
			loader.setQuery(String.format("SELECT %s FROM %s %s %s", select, this.tableName, whereClause, groupBy));
			if (loader.getDataSet() == null) {
				loader.reset();
				return null;
			} else {
				Ads data = new Ads(loader.getDataSet());
				// Database replaces spaces with underscores in names on store, so we need to change that back
				for (int attrIndex = 0; attrIndex < data.numAttributes(); attrIndex++) {
					Attribute attribute = data.attribute(attrIndex);
					if (attribute.name().indexOf("_") > 0) {
						data.renameAttribute(attribute, attribute.name().replaceAll("_", " "));
					}
				}
				loader.reset();
				return data;
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Return the sum of the metrics for a specific attribute (e.g. Body).
	 *
	 * @param attribute Name of attribute to aggregate
	 * @return Aggregated ads
	 */
	public Ads getAggregate(String attribute)
	{
		attribute = attribute.replaceAll(" ", "_");
		return this.get(attribute + ", SUM(Clicks_Count) AS Clicks_Count, SUM(Impressions) AS Impressions", attribute);
	}
	
	/**
	 * Return an instance of Ads that only contains target values for Gender, Age Min and Age Max.
	 *
	 * @return Targets in data set
	 */
	public Ads getTargets()
	{
		return this.get("Gender, Age_Min, Age_Max", "");
	}

	@Override
	public void where(String key, String value)
	{
		if (key.equalsIgnoreCase("Age")) {
			this.where("Age Min", value.substring(0, value.indexOf("-")));
			this.where("Age Max", value.substring(value.indexOf("-")+1));
			return;
		}
		this.where.add(key.replaceAll(" ", "_"));
		this.where.add(value);
	}

	@Override
	public void resetWhere()
	{
		this.where = new LinkedList<String>();
	}
	
	/**
	 * Private helper method for generating the SQL where clause.
	 * 
	 * @return SQL WHERE clause
	 */
	private String createWhereClause()
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("WHERE Impressions > 0");
		
		if (!this.where.isEmpty()) {
			boolean key = true;
			buffer.append(" AND ");
			for (String item : where) {
				if (key) {
					buffer.append(item);
				} else {
					buffer.append(" = ");
					if (item.matches("-?\\d+(\\.\\d+)?")) {
						buffer.append(item);
					} else {
						buffer.append("'").append(item).append("'");
					}
					buffer.append(" AND ");
				}
				key = !key;
			}
			return buffer.substring(0, buffer.length()-5);
		}
		
		return buffer.toString();
	}

}