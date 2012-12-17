package adstimator.data;

/**
 * Interface for interacting with persistent data storage.
 * 
 * @author erikbrannstrom
 */
public interface AdStorage
{
	/**
	 * Append the specified ads to persistent storage.
	 * 
	 * @param ads 
	 */
	public void add(Ads ads);
	
	/**
	 * Return all ads which match the where clauses given. If the where method has not been used, all ads in storage
	 * should be returned.
	 * 
	 * @return Ads matching where clause, null if no ads are found
	 */
	public Ads get();
	
	/**
	 * Add a where clause. This affects which ads will be returned by the next call to get().
	 * 
	 * @param key Name of attribute
	 * @param value Value of attribute
	 */
	public void where(String key, String value);
	
	/**
	 * Clear all where clauses.
	 */
	public void resetWhere();
}