package adstimator.evaluation;

import adstimator.data.DataSplitter;
import adstimator.core.Estimator;
import adstimator.data.Ad;
import adstimator.data.Ads;
import java.util.*;
import weka.core.*;

public class ClassifierEvaluator extends Evaluator
{
	private Ads data;
	private int runs;
	private double delta;
	private Map<String, List<String>> classifiers;
	private Map<String, Double> results;

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

	public String description()
	{
		return "Evaluation of the performance of different classifiers";
	}

	public String result()
	{
		this.evaluate();
		StringBuffer buf = new StringBuffer();

		for (String className : this.results.keySet()) {
			Double rate = this.results.get(className);
			buf.append(String.format("%s: %.5f", className, this.results.get(className)));
		}

		return buf.toString();
	}

	public double successRate(String className)
	{
		this.evaluate();
		return this.results.get(className);
	}

	public void delta(double d)
	{
		this.delta = d;
	}

	public double delta()
	{
		return this.delta;
	}

	private void evaluate()
	{
		if (this.results != null) {
			return;
		}

		this.results = new HashMap<String, Double>();
		int count = 0;

		for (int i = 0; i < runs; i++) {
			DataSplitter splitter = new DataSplitter(data, 0.1);
			Instances estimation = splitter.split();
			Instances training = splitter.remaining();

			estimation.deleteAttributeAt(estimation.attribute("Actions").index());
			estimation.deleteAttributeAt(estimation.attribute("Impressions").index());
			estimation.insertAttributeAt(new Attribute("ActionRate"), estimation.numAttributes());
			estimation.setClass(estimation.attribute("ActionRate"));

			for (String className : this.classifiers.keySet()) {
				String[] opts = null;
				if (this.classifiers.get(className) != null) {
					opts = this.classifiers.get(className).toArray(new String[0]);
				}
				Estimator est = Estimator.factory(training, className, opts);

				double errors = 0.0;
				for (Instance suggestion : estimation) {
					double estimate = est.estimate(suggestion);
					Ad match = (Ad)this.data.findMatch(suggestion);

					double real = match.actionRate();
					if (real < estimate - this.delta() || real > estimate + this.delta()) {
						errors++;
					}
				}
				Double totalErrors = this.results.get(className);
				if (totalErrors == null) {
					this.results.put(className, errors);
				} else {
					this.results.put(className, totalErrors+errors);
				}
			}
			count += estimation.numInstances();
		}

		// Divide number of errors by total number of trials. The success rate is then 1 minus that value.
		for (String className : this.results.keySet()) {
			Double totalErrors = this.results.get(className);
			this.results.put(className, 1.0-totalErrors/count);
		}
	}

}