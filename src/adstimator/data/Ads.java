package adstimator.data;

import java.util.*;
import weka.core.*;

/**
 * Class for representing a set of ad campaigns.
 * 
 * The class is an extension of Weka's Instances class with a few additions that are specific to ads and their metrics.
 * For example, an instance of ads can be converted from the standard metrics of clicks and impressions to the actual
 * click rate (i.e. clicks divided by impressions).
 * 
 * @author erikbrannstrom
 */
public class Ads extends Instances
{
	/**
	 * List of target properties.
	 */
	public static final List<String> TARGETS = Arrays.asList("Gender", "Age Min", "Age Max");
	/**
	 * List of ad properties.
	 */
	public static final List<String> AD = Arrays.asList("Body", "Image Hash");
	/**
	 * List of metrics.
	 */
	public static final List<String> METRICS = Arrays.asList("Clicks Count", "Impressions", "Click Rate");

	/**
	 * Create a new set of ads from instances.
	 * 
	 * @param inst 
	 */
	public Ads(Instances inst)
	{
		super(inst);

		// Make sure we have attribute for metrics, otherwise add rate attribute
		boolean hasMetrics = false;
		for (int i = 0; i < this.numAttributes(); i++) {
			if ( Ads.METRICS.contains(this.attribute(i).name()) ) {
				hasMetrics = true;
				break;
			}
		}
		if (!hasMetrics) {
			this.insertAttributeAt(new Attribute("Click Rate"), this.numAttributes());
		}
	}

	/**
	 * Find an instance matching another instance.
	 * 
	 * An instance is considered to be a match if they have the same ad properties, and ignores targeting and metrics.
	 * Only the first match is returned.
	 * 
	 * @param original
	 * @return First matching ad, null if no match could be found
	 */
	public Instance findMatch(Instance original)
	{
		for (Instance test : this) {
			boolean match = true;
			for (int i = 0; i < this.numAttributes(); i++) {
				if (Ads.TARGETS.contains(this.attribute(i).name()) || Ads.METRICS.contains(this.attribute(i).name())) {
					continue;
				}
				if (Math.abs(original.value(i) - test.value(i)) > 0.00001) {
					match = false;
					break;
				}
			}
			if (match) {
				return test;
			}
		}
		return null;
	}

	/**
	 * Convert the whole set of instances to the click rate format by calculating the rate and then removing the old
	 * attributes.
	 */
	public void convertToRate()
	{
		if (this.attribute("Click Rate") != null) {
			return;
		}

		// Add action rate
		this.insertAttributeAt(new Attribute("Click Rate"), this.numAttributes());
		Attribute ar = this.attribute("Click Rate");
		Attribute actions = this.attribute("Clicks_Count");
		if (actions == null) {
			actions = this.attribute("Clicks Count");
		}
		Attribute impressions = this.attribute("Impressions");

		if (actions == null || impressions == null) {
			throw new RuntimeException("The required attributes could not be found in data set.");
		}

		for (int i = 0; i < this.numInstances(); i++) {
			Instance inst = this.get(i);
			double val = inst.value(actions)/inst.value(impressions);
			inst.setValue(ar, val);
		}

		this.setClass(ar);
		this.deleteAttributeAt(impressions.index());
		this.deleteAttributeAt(actions.index());
	}

	/**
	 * Get the average action rate of the whole data set.
	 * 
	 * @return Average action rate
	 */
	public double averageActionRate()
	{
		Attribute ar = this.attribute("Click Rate");
		if ( ar != null) {
			double sum = 0.0;
			for (Instance ad : this) {
				sum += ad.value(ar);
			}
			return sum/this.numInstances();
		} else {
			Attribute attrActions = this.attribute("Clicks Count");
			Attribute attrImpressions = this.attribute("Impressions");
			double actions = 0.0;
			double impressions = 0.0;
			for (Instance ad : this) {
				actions += ad.value(attrActions);
				impressions += ad.value(attrImpressions);
			}
			return actions/impressions;
		}
	}

}