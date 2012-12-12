package adstimator.evaluation;

import adstimator.core.Estimator;
import adstimator.data.Ads;
import adstimator.data.DataSplitter;
import java.util.*;
import weka.core.*;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * Class for evaluating different selection strategies.
 * 
 * The data set will be split into a training set (90% of the original data, randomly selected) and a validation set
 * (10%). The classifier then estimates a click rate for each instance in the validation set and selects the N highest
 * ones, where N is the strategy. The actual click rate is then gathered and the average is calculated. This is repeated
 * the number of times specified in the constructor, and the average is returned.
 * 
 * @author erikbrannstrom
 */
public class StrategyEvaluator
{
	private Ads data;
	private int runs;
	private Map<String, List<String>> classifiers;

	/**
	 * Create a new evaluator, with the classifiers set to logistic regression, SMO regression, IBk and REPTree.
	 * 
	 * @param data
	 * @param runs 
	 */
	public StrategyEvaluator(Ads data, int runs)
	{
		this(data, runs, null);
		this.classifiers = new HashMap<String, List<String>>();
		this.classifiers.put("weka.classifiers.functions.Logistic", Arrays.asList("-R", "1000"));
		this.classifiers.put("weka.classifiers.functions.SMOreg", null);
		this.classifiers.put("weka.classifiers.lazy.IBk", null);
		this.classifiers.put("weka.classifiers.trees.REPTree", null);
	}

	/**
	 * Create a new evaluator.
	 * 
	 * @param data data set used for training and validation
	 * @param runs number of times each strategy is tested
	 * @param classifiers classifiers to be evaluated
	 */
	public StrategyEvaluator(Ads data, int runs, Map<String, List<String>> classifiers)
	{
		this.data = data;
		this.runs = runs;
		this.classifiers = classifiers;
	}

	/**
	 * Evaluates selection strategies for the classifiers defined.
	 * 
	 * The strategies N = 1, 2, ..., <i>strategy</i>, either weighted or unweighted, are evaluated. The results are
	 * returned as a map where the name of the classifier is the key and the value is a list of the average real click
	 * rate for that strategy (note that the result for strategy 1 is at index 0 in the list).
	 * 
	 * @param strategy
	 * @param weighted
	 * @return 
	 */
	public Map<String, List<Double>> evaluate(int strategy, boolean weighted)
	{
		Map<String, List<Double>> results = new HashMap<String, List<Double>>();

		for (String className : this.classifiers.keySet()) {
			List<Double> strategyList = new LinkedList<Double>();
			for (int i = 0; i < strategy; i++) {
				strategyList.add(0.0);
			}
			
			for (int i = 0; i < runs; i++) {
				DataSplitter splitter = new DataSplitter(this.data, 0.1);
				Instances estimation = splitter.split();
				Instances training = splitter.remaining();
				Ads validation = new Ads(estimation);
				validation.convertToRate();

				estimation.deleteAttributeAt(estimation.attribute("Clicks Count").index());
				estimation.deleteAttributeAt(estimation.attribute("Impressions").index());
				estimation.insertAttributeAt(new Attribute("Click Rate"), estimation.numAttributes());
				estimation.setClass(estimation.attribute("Click Rate"));

				// Create the estimator
				String[] opts = null;
				if (this.classifiers.get(className) != null) {
					opts = this.classifiers.get(className).toArray(new String[0]);
				}
				Estimator est = Estimator.factory(new Instances(training), className, opts);

				// Estimate suggestions
				for (Instance suggestion : estimation) {
					double estimate = est.estimate(suggestion);
					suggestion.setClassValue(estimate);
				}
				
				// Sort to get highest first
				this.sort(estimation);
				
				// Calculate average real action rate for selected ads according to strategy
				double sumReal = 0.0;
				double sumEst = 0.0;
				for (int n = 0; n < strategy; n++) {
					Instance suggestion = estimation.get(n);
					Instance real = validation.findMatch(suggestion);
					if (weighted) {
						sumEst += suggestion.classValue();
						sumReal += suggestion.classValue()*real.classValue();
						strategyList.set(n, strategyList.get(n)+sumReal/sumEst);
					} else {
						sumReal += real.classValue();
						strategyList.set(n, strategyList.get(n)+sumReal/(n+1));
					}
				}
				
				// Clear estimations for next classifier
				for (Instance instance : estimation) {
					instance.setClassMissing();
				}
			}
			for (int strat = 0; strat < strategyList.size(); strat++) {
				strategyList.set(strat, strategyList.get(strat)/runs);
			}
			results.put(className, strategyList);
		}
		return results;
	}
	
	/**
	 * Private helper method for sorting instances by click rate.
	 * 
	 * @param instances Instances to be sorted. Order will obviously be modified.
	 */
	private void sort(Instances instances)
	{
		Comparator<Instance> rateComparator = new Comparator<Instance>() {
			@Override
			public int compare(Instance a, Instance b) {
				Attribute ctrMean = a.dataset().attribute("Click Rate");

				if (a.value(ctrMean) < b.value(ctrMean)) {
					return 1;
				} else if (a.value(ctrMean) > b.value(ctrMean)) {
					return -1;
				} else {
					return 0;
				}
			}
		};

		Collections.sort(instances, rateComparator);
	}
	
	/**
	 * Main method.
	 * 
	 * @param args First argument must be data file to run evaluation on.
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		DataSource source = new DataSource(args[0]);
		Ads data = new Ads(source.getDataSet());

		StrategyEvaluator strat = new StrategyEvaluator(data, 5000);
		Map<String, List<Double>> result = strat.evaluate(3, true);
		for (String string : result.keySet()) {
			System.out.printf("%s\n", string);
			for (int i = 1; i <= 3; i++) {
				System.out.printf("Strategy %d: %.6f\n", i, result.get(string).get(i-1));
			}
		}

	}

}