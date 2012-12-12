package adstimator.core;

import weka.classifiers.AbstractClassifier;
import weka.core.*;

/**
 * Abstract class for estimators.
 * 
 * The static factory will most often be used to initialize classifiers in order to not have to know beforehand
 * whether a classifier is numeric or nominal.
 * 
 * @author erikbrannstrom
 */
public abstract class Estimator
{
	protected Instances knowledge;

	/**
	 * Constructor for abstract estimator class.
	 *
	 * The knowledge instances are expected to have a Clicks Count attribute and an Impressions attribute.
	 */
	protected Estimator(Instances knowledge)
	{
		this.knowledge(knowledge);
	}

	/**
	 * Factory for easily creating the numeric and nominal implementations of this abstract class.
	 * 
	 * The method automatically checks the capabilities of the classifier to see if it expects numeric or nominal data.
	 * It is then initialized with the knowledge and options given as parameters.
	 * 
	 * @param knowledge training data
	 * @param className full weka class name (e.g. weka.classifiers.lazy.IBk)
	 * @param options options for the classifier, as given to its main method. Can be null.
	 * @return 
	 */
	public static Estimator factory(Instances knowledge, String className, String[] options)
	{
		try {
			AbstractClassifier classifier = (AbstractClassifier)AbstractClassifier.forName(className, options);
			if (classifier.getCapabilities().handles(Capabilities.Capability.NUMERIC_CLASS)) {
				return new NumericEstimator(knowledge, classifier);
			} else {
				return new NominalEstimator(knowledge, classifier);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Abstract method which takes the training data, which has metrics in the form of clicks and impressions, and
	 * modifies it to be usable by the classifier. See implementations for examples.
	 * 
	 * This method MUST set the protected knowledge variable to the modified data set.
	 * 
	 * @param knowledge Data to be parsed
	 */
	protected abstract void knowledge(Instances knowledge);
	
	/**
	 * Method which returns an estimated value for the specified instance, based on the training data.
	 * 
	 * @param instance Ad to be estimated
	 * @return Estimated probability of click
	 */
	public abstract double estimate(Instance instance);

}