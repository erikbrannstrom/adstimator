package adstimator.core;

import adstimator.data.Ads;
import java.util.*;
import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Ad factory which creates combinations of all ad properties available in the data set provided through the 
 * constructor.
 * 
 * @author erikbrannstrom
 */
public class CombinationAdFactory implements AdFactory
{
	private Ads data;
	private Instances suggestions;

	public CombinationAdFactory(Instances data)
	{
		this.data = new Ads(data);
	}

	/**
	 * Return a list of ads whose combination of ad properties does not exist in the provided data.
	 *
	 * @return Suggested ads
	 */
	@Override
	public Ads all()
	{
		if (this.suggestions == null) {
			this.createSuggestions();
		}
		return new Ads(this.suggestions);
	}

	/**
	 * Private helper method for creating the ads to be suggested.
	 */
	private void createSuggestions()
	{
		// Create all combinations
		this.suggestions = new Instances(this.data, 0);
		DenseInstance inst = new DenseInstance(this.suggestions.numAttributes());
		inst.setDataset(this.suggestions);
		this.combineInstances(inst, 0);

		// Remove existing campaigns
		Iterator<Instance> it = this.suggestions.iterator();
		while (it.hasNext()) {
			Instance suggestion = it.next();
			Instance match = this.data.findMatch(suggestion);
			if (match != null) {
				it.remove();
			}
		}

		// Find all attributes that are campaign properties (target, ad or metrics)
		List<Integer> keepIndices = new LinkedList<Integer>();
		for (int i = 0; i < this.suggestions.numAttributes(); i++) {
			String attrName = this.suggestions.attribute(i).name();
			if ( Ads.AD.contains(attrName.replaceAll("_", " ")) ) {
				keepIndices.add(i+1); // String based indices start at 1, not 0
			}
		}

		// Keep only attributes that have been identified as campaign properties
		try {
			Remove removeFilter = new Remove();
			removeFilter.setAttributeIndices(keepIndices.toString().replaceAll("[^0-9,]", ""));
			removeFilter.setInvertSelection(true);
			removeFilter.setInputFormat(this.suggestions);

			// Filter instances
			this.suggestions = Filter.useFilter(this.suggestions, removeFilter);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	/*
	 * Private helper method for recursively creating ad suggestions.
	 */
	private void combineInstances(Instance inst, int col)
	{
		if (col == this.data.numAttributes()-1) {
			// If last column, we are done.
			this.suggestions.add(inst);
			return;
		} else if (!Ads.AD.contains(this.data.attribute(col).name())) {
			// Ignore properties that are not specifying ad content
			this.combineInstances(inst, col+1);
			return;
		}

		// For each value the current attribute can take, do a recursive call to this method to set the next attribute
		for (int i = 0; i < this.data.attribute(col).numValues(); i++) {
			DenseInstance newInst = new DenseInstance(inst);
			newInst.setDataset(inst.dataset());
			newInst.setValue(col, (double)i);
			this.combineInstances(newInst, col+1);
		}
	}

}