package adstimator.data;

import weka.core.*;
import java.util.*;

public class Ads extends Instances
{
	public static final List<String> TARGETS = Arrays.asList("Gender", "Age Min", "Age Max");
	public static final List<String> AD = Arrays.asList("Body", "Image Hash");
	public static final List<String> METRICS = Arrays.asList("Clicks Count", "Impressions", "Click Rate");

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
			Attribute attrActions = this.attribute("Actions");
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