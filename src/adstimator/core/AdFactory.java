package adstimator.core;

import adstimator.data.Ads;

/**
 * Interface for classes that provide ads to be estimated.
 * 
 * @author erikbrannstrom
 */
public interface AdFactory
{
	public Ads all();
}