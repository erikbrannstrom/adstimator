package adstimator.data;

import adstimator.data.Ads;
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
		String whereClause = "";

		StringBuffer buffer = new StringBuffer();
		buffer.append("WHERE Impressions > 0");
		if (this.where.size() != 0) {
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
			whereClause = buffer.substring(0, buffer.length()-5);
		}

		try {
			DatabaseLoader loader = new DatabaseLoader();
			loader.connectToDatabase();
			loader.setQuery(String.format("SELECT Body, Image_Hash, Clicks_Count, Impressions FROM %s %s", this.tableName, whereClause));
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

	public void where(String key, String value)
	{
		this.where.add(key);
		this.where.add(value);
	}

	public void resetWhere()
	{
		this.where = new LinkedList<String>();
	}

}