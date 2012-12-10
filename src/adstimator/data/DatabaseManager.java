package adstimator.data;

import java.util.*;
import weka.core.converters.DatabaseLoader;
import weka.core.converters.DatabaseSaver;

public class DatabaseManager implements DataManager
{
	private String tableName;
	private List<String> where;

	public DatabaseManager(String tableName)
	{
		this.tableName = tableName;
		this.where = new LinkedList<String>();
	}

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
	
	public Ads get()
	{
		return this.get("Body, Image_Hash, Clicks_Count, Impressions", "");
	}

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
				Ads ads = new Ads(loader.getDataSet());
				loader.reset();
				return ads;
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public Ads getAggregate(String attribute)
	{
		attribute = attribute.replaceAll(" ", "_");
		return this.get(attribute + ", AVG(Clicks_Count) AS Clicks_Count, AVG(Impressions) AS Impressions", attribute);
	}
	
	public Ads getTargets()
	{
		return this.get("Gender, Age_Min, Age_Max", "");
	}

	public void where(String key, String value)
	{
		this.where.add(key.replaceAll(" ", "_"));
		this.where.add(value);
	}

	public void resetWhere()
	{
		this.where = new LinkedList<String>();
	}
	
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