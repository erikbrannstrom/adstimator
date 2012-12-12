package adstimator.gui.models;

import java.util.List;
import java.util.Map;

/**
 * Interface used when an object have selectable objects, and those objects should be exportable.
 *
 * @author erikbrannstrom
 */
public interface SelectionInterface
{
	/**
	 * Return a list as long as the number of objects selected, with each map representing one of those objects.
	 * 
	 * @return Key-value description of selected objects
	 */
	public List<Map<String, String>> exportSelected();
}
