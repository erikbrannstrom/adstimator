package adstimator.core;

import java.util.*;
import weka.classifiers.*;
import weka.core.*;

/**
 * Nominal estimator class.
 * 
 * Used for estimating ads with a nominal classifier, such as logistic regression.
 * 
 * @author erikbrannstrom
 */
public class NominalEstimator extends Estimator
{
	private Classifier classifier;

	/**
	 * Create a new nominal estimator. Most often used indirectly by calling the static factory on Estimator.
	 * 
	 * @param knowledge training data
	 * @param classifier classifier instance (untrained)
	 */
	public NominalEstimator(Instances knowledge, Classifier classifier)
	{
		super(knowledge);
		this.classifier(classifier);
	}

	/**
	 * Converts each instance into two separate weighted instances, one for action taken and one for action not taken.
	 */
	@Override
	protected void knowledge(Instances knowledge)
	{
		// If the action attribute already exists, we assume the data has already been adapted for nominal classifiers
		if (knowledge.attribute("Action") != null) {
			this.knowledge = knowledge;
			return;
		}

		Attribute actions = knowledge.attribute("Clicks Count");
		Attribute impressions = knowledge.attribute("Impressions");

		if (actions == null || impressions == null) {
			throw new RuntimeException("The required attributes could not be found in data set.");
		}

		// Add clicked attribute
		List<String> yesNo = new LinkedList<String>();
		yesNo.add("yes");
		yesNo.add("no");
		knowledge.insertAttributeAt(new Attribute("Action", yesNo), knowledge.numAttributes());
		Attribute action = knowledge.attribute("Action");

		// Each ad in the training set will be duplicated, with one instance for the clicks and one for the impressions
		// which did not lead to clicks. The respective instances are weighted according to these numbers.
		int length = knowledge.numInstances();
		for (int i = 0; i < length; i++) {
			Instance yes = knowledge.instance(i);
			Instance no = (Instance)yes.copy();
			yes.setValue(action.index(), "yes");
			no.setValue(action.index(), "no");
			yes.setWeight(1.0*yes.value(actions)+1);
			no.setWeight(1.0*yes.value(impressions)-yes.value(actions)+1);
			knowledge.add(no);
		}

		// Set action as class and remove the old metrics
		knowledge.setClass(action);
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
	 * Estimate the probability than the instance belongs to the class "yes" (i.e. being clicked).
	 * 
	 * @param instance ad to be estimated
	 * @return probability of click
	 */
	@Override
	public double estimate(Instance instance)
	{
		try {
			double[] distr = this.classifier.distributionForInstance(instance);
			return distr[this.knowledge.classAttribute().indexOfValue("yes")];
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}