package adstimator.core;

import weka.classifiers.*;
import weka.core.*;

/**
 * Numeric estimator.
 * 
 * Handles classifiers that expect the class value to be numeric, such as linear regression for example.
 * 
 * @author erikbrannstrom
 */
public class NumericEstimator extends Estimator
{
	private Classifier classifier;

	/**
	 * Create a new numeric estimator. Most often used indirectly by calling the static factory on Estimator.
	 * 
	 * @param knowledge training data
	 * @param classifier classifier instance (untrained)
	 */
	public NumericEstimator(Instances knowledge, Classifier classifier)
	{
		super(knowledge);
		this.classifier(classifier);
	}

	/**
	 * Converts the Clicks Count and Impressions to a single Click Rate attribute.
	 */
	@Override
	protected void knowledge(Instances knowledge)
	{
		// If click rate exists, we assume we are done
		if (knowledge.attribute("Click Rate") != null) {
			this.knowledge = knowledge;
			return;
		}

		// Add click rate attribute
		knowledge.insertAttributeAt(new Attribute("Click Rate"), knowledge.numAttributes());
		Attribute ar = knowledge.attribute("Click Rate");
		Attribute actions = knowledge.attribute("Clicks Count");
		Attribute impressions = knowledge.attribute("Impressions");

		if (actions == null || impressions == null) {
			throw new RuntimeException("The required attributes could not be found in data set.");
		}

		// Calculate click rate for all instances
		for (int i = 0; i < knowledge.numInstances(); i++) {
			Instance inst = knowledge.get(i);
			double val = inst.value(actions)/inst.value(impressions);
			inst.setValue(ar, val);
		}

		// Set class and remove old metric attributes
		knowledge.setClass(ar);
		knowledge.deleteAttributeAt(impressions.index());
		knowledge.deleteAttributeAt(actions.index());

		this.knowledge = knowledge;
	}

	/**
	 * Set and build classifier.
	 * 
	 * @param classifier 
	 */
	private void classifier(Classifier classifier)
	{
		this.classifier = classifier;
		try {
			this.classifier.buildClassifier(this.knowledge);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Estimate the numeric class value for an instance.
	 * 
	 * @param instance ad to be estimated
	 * @return probability of click
	 */
	@Override
	public double estimate(Instance instance)
	{
		try {
			return this.classifier.classifyInstance(instance);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}