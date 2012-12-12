package adstimator.gui.models;

import java.util.Map;

/**
 * Interface used for classes that should be able to return a map of a target. The key should be the target type and
 * is mapped to the value which that target type should have.
 *
 * @author erikbrannstrom
 */
public interface TargetInterface
{
	/**
	 * Return a key-value map describing a target.
	 * 
	 * @return Description of a target
	 */
	public Map<String, String> currentTarget();
}
