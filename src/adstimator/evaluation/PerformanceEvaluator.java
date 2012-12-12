package adstimator.evaluation;

import adstimator.core.Estimator;
import cern.jet.random.Normal;
import cern.jet.random.engine.RandomEngine;
import java.util.*;
import weka.core.*;

/**
 * Evaluates the time performance of a classifier.
 *
 * A fake data set is generated with a specified number of nominal values and occurrences for each combination. The time
 * required for the estimator to be built with the training data and estimate the values of a number of instances (same
 * as the number of nominal values) are then measured.
 *
 * The data set generated has two attributes, so that the total number of instances generated are N^2*M where N is the
 * number of nominal values and M the occurrences of each instance.
 *
 * @author erikbrannstrom
 */
public class PerformanceEvaluator extends Evaluator
{

	private int nominalValues, occurrences;
	private long time;
	private String classifier;
	private String[] options;

	/**
	 * Create a new performance evaluator.
	 *
	 * @param nominalValues Number of nominal values (for each of two attributes)
	 * @param occurrences Occurrences of each instance combination
	 * @param classifier Classifier name (full Weka name)
	 * @param options Can be null
	 */
	public PerformanceEvaluator(int nominalValues, int occurrences, String classifier, String[] options)
	{
		this.nominalValues = nominalValues;
		this.occurrences = occurrences;
		this.classifier = classifier;
		this.options = options;
		this.time = -1;
	}

	/**
	 * Private method for running the performance measure.
	 */
	private void evaluate()
	{
		// Generate a large data set, min @nominalValues texts and images, each combination with random CTR
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		ArrayList<Attribute> testAttributes = new ArrayList<Attribute>();
		List<String> textValues = new LinkedList<String>();
		List<String> imageValues = new LinkedList<String>();
		for (int i = 1; i <= this.nominalValues; i++) {
			textValues.add("Text-" + i);
			imageValues.add("Image-" + i);
		}
		Attribute bodyAttr = new Attribute("Body", textValues);
		Attribute imageAttr = new Attribute("Image Hash", imageValues);
		Attribute clicks = new Attribute("Clicks Count");
		Attribute impressions = new Attribute("Impressions");
		Attribute actionRate = new Attribute("Click Rate");
		attributes.add(bodyAttr);
		testAttributes.add(bodyAttr);
		attributes.add(imageAttr);
		testAttributes.add(imageAttr);
		attributes.add(clicks);
		attributes.add(impressions);
		testAttributes.add(actionRate);

		// Randomly assign clicks and impressions from a normal distribution
		Normal ctrDistr = new Normal(0.02, 0.01, RandomEngine.makeDefault());
		Normal impressionDistr = new Normal(150000.0, 30000.0, RandomEngine.makeDefault());
		Instances trainingData = new Instances("Generated Training Data", attributes, nominalValues);
		Instances testData = new Instances("Generated Test Data", testAttributes, nominalValues);
		testData.setClassIndex(testData.numAttributes() - 1);

		// Create all nominal values
		for (int i = 1; i <= this.nominalValues; i++) {
			String body = "Text-" + i;
			for (int j = 1; j <= this.nominalValues; j++) {
				String image = "Image-" + j;
				if (i != j) {
					// Store N(N-1) instances in training data
					for (int occurence = 0; occurence < this.occurrences; occurence++) {
						Instance instance = new DenseInstance(4);
						instance.setDataset(trainingData);
						instance.setValue(bodyAttr, body);
						instance.setValue(imageAttr, image);
						double ctr = Math.abs(ctrDistr.nextDouble());
						int impressionCount = (int) Math.round(impressionDistr.nextDouble(150000.0, 30000.0));
						instance.setValue(impressions, impressionCount + 1);
						instance.setValue(clicks, (int) Math.round(ctr * impressionCount) + 1); // Cannot be 0
						trainingData.add(instance);
					}
				} else {
					// Store N instances in test data
					Instance instance = new DenseInstance(3);
					instance.setDataset(testData);
					instance.setValue(bodyAttr, body);
					instance.setValue(imageAttr, image);
					testData.add(instance);
				}
			}
		}

		// Mark time
		long start = System.currentTimeMillis();

		// Create estimator
		Estimator est = Estimator.factory(trainingData, this.classifier, this.options);
		for (Instance testInstance : testData) {
			testInstance.setClassValue(est.estimate(testInstance));
		}

		// Return time taken to build classifier
		this.time = System.currentTimeMillis() - start;
	}

	/**
	 * Get time required for evaluation.
	 *
	 * @return Time in milliseconds
	 */
	public long time()
	{
		if (this.time == -1) {
			this.evaluate();
		}
		return this.time;
	}

	/**
	 * Return text containing the time in seconds required to build classifier and estimate instances.
	 *
	 * @return result description
	 */
	@Override
	public String result()
	{
		if (this.time == -1) {
			this.evaluate();
		}
		return String.format("Classifier was built and test data estimated in %.4f seconds.", this.time / 1000.0);
	}

	/**
	 * Textual description for this evaluator.
	 *
	 * @return
	 */
	@Override
	public String description()
	{
		return "Evaluate the performance of an estimator by building the classifier from a large generated data set.";
	}

	/**
	 * Main method.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		for (int i = 10; i <= 10; i = i + 10) {
			long sumTime = 0;
			for (int trial = 1; trial <= 3; trial++) {
				PerformanceEvaluator evaluator = new PerformanceEvaluator(10, i, "weka.classifiers.functions.Logistic",
						Arrays.asList("-R", "1000").toArray(new String[0]));
				sumTime += evaluator.time();
			}
			System.out.println(i + ": " + sumTime / 3.0);
		}
	}
}