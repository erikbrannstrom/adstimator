package adstimator.evaluation;

import adstimator.core.Estimator;
import adstimator.data.Ads;
import adstimator.evaluation.util.DataSplitter;
import java.util.*;
import weka.core.*;
import weka.core.converters.ConverterUtils;

/**
 * Evaluates classifiers in terms of their accuracy of estimation.
 * 
 * The classifiers are logistic regression, SMO regression, linear regression, M5P, IBk and REPTree. The success rate
 * is defined as the percentage of times the estimated value differs by at most the specified delta in either direction.
 * 
 * @author erikbrannstrom
 */
public class ClassifierEvaluator extends Evaluator
{
	private Ads data;
	private int runs;
	private double delta;
	private Map<String, List<String>> classifiers;
	private Map<String, Double> results;

	/**
	 * Create a new classifier evaluator.
	 * 
	 * @param data Data for training and validation
	 * @param runs Number of trials
	 * @param delta Delta defining success interval
	 */
	public ClassifierEvaluator(Ads data, int runs, double delta)
	{
		this.data = data;
		this.runs = runs;
		this.delta = delta;
		this.classifiers = new HashMap<String, List<String>>();
		this.classifiers.put("weka.classifiers.functions.Logistic", Arrays.asList("-R", "1000"));
		this.classifiers.put("weka.classifiers.functions.SMOreg", null);
		this.classifiers.put("weka.classifiers.functions.LinearRegression", null);
		this.classifiers.put("weka.classifiers.trees.M5P", null);
		this.classifiers.put("weka.classifiers.lazy.IBk", null);
		this.classifiers.put("weka.classifiers.trees.REPTree", null);
	}

	/**
	 * Textual description for this evaluator.
	 * 
	 * @return 
	 */
	@Override
	public String description()
	{
		return "Evaluation of the performance of different classifiers";
	}

	/**
	 * Returns the results of this evaluator as a string.
	 * 
	 * @return 
	 */
	@Override
	public String result()
	{
		this.evaluate();
		StringBuilder buf = new StringBuilder();

		for (String className : this.results.keySet()) {
			Double rate = this.results.get(className);
			buf.append(String.format("%s: %.5f\n", className, rate));
		}

		return buf.toString();
	}

	/**
	 * Get success rate for the specified classifier.
	 * 
	 * @param className Classifier name (full Weka name)
	 * @return success percentage
	 */
	public double successRate(String className)
	{
		this.evaluate();
		return this.results.get(className);
	}

	/**
	 * Private method which does the actual evaluation. Stores the results so it only has to be processed once.
	 */
	private void evaluate()
	{
		if (this.results != null) {
			return;
		}

		this.results = new HashMap<String, Double>();
		int count = 0;

		// Repeat trial the number of times specified in constructor
		for (int i = 0; i < runs; i++) {
			// Split data, 90% training and 10% validation/estimation
			DataSplitter splitter = new DataSplitter(data, 0.1);
			Instances estimation = splitter.split();
			Instances training = splitter.remaining();

			// Change format of estimation set
			estimation.deleteAttributeAt(estimation.attribute("Clicks Count").index());
			estimation.deleteAttributeAt(estimation.attribute("Impressions").index());
			estimation.insertAttributeAt(new Attribute("Click Rate"), estimation.numAttributes());
			estimation.setClass(estimation.attribute("Click Rate"));

			// Perform test for each classifier
			for (String className : this.classifiers.keySet()) {
				String[] opts = null;
				if (this.classifiers.get(className) != null) {
					opts = this.classifiers.get(className).toArray(new String[0]);
				}
				// Create estimator, using a new copy of the training data since it will be modified
				Estimator est = Estimator.factory(new Instances(training), className, opts);

				// For each ad in the estimation set, estimate and compare to real value
				double errors = 0.0;
				for (Instance suggestion : estimation) {
					double estimate = est.estimate(suggestion);
					Instance match = this.data.findMatch(suggestion);

					double real = match.value(match.dataset().attribute("Clicks Count"))
								  / match.value(match.dataset().attribute("Impressions"));
					if (real < estimate - this.delta || real > estimate + this.delta) {
						errors++;
					}
				}
				
				// Store number of errors
				Double totalErrors = this.results.get(className);
				if (totalErrors == null) {
					this.results.put(className, errors);
				} else {
					this.results.put(className, totalErrors+errors);
				}
			}
			// Number of instances estimated and compared
			count += estimation.numInstances();
		}

		// Divide number of errors by total number of trials. The success rate is then 1 minus that value.
		for (String className : this.results.keySet()) {
			Double totalErrors = this.results.get(className);
			this.results.put(className, 1.0-totalErrors/count);
		}
	}
	
	/**
	 * Run evaluator with specified data.
	 * 
	 * @param args Requires the first (and only) parameter to be the data file (ARFF/CSV) to be used
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception
	{
		ConverterUtils.DataSource source = new ConverterUtils.DataSource(args[0]);
		Ads data = new Ads(source.getDataSet());
		double k = 0.20;
		ClassifierEvaluator evaluator = new ClassifierEvaluator(data, 500, data.averageActionRate()*k);
		System.out.println(evaluator.result());
	}

}