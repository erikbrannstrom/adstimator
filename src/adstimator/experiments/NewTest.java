package adstimator.experiments;

import adstimator.data.DataSplitter;
import adstimator.core.Estimator;
import adstimator.evaluation.EstimationEvaluator;
import java.util.*;
import weka.core.*;
import weka.core.converters.ConverterUtils.DataSource;

public class NewTest
{
	public static void main(String[] args) throws Exception {
		DataSource source = new DataSource(args[0]);
		Instances data = source.getDataSet();

		int runs = 1000;
		if (args.length > 1) {
			runs = Integer.parseInt(args[1]);
		}

		List<Double> ks = new LinkedList<Double>();
		ks.add(1.0);
		/*for (double add = 0.01; add < 0.21; add += 0.01) {
			ks.add(add);
		}
		ks.add(0.01);
		ks.add(0.05);
		ks.add(0.10);
		ks.add(0.20);*/

		Map<String, List<String>> classifiers = new HashMap<String, List<String>>();
		classifiers.put("weka.classifiers.functions.Logistic", Arrays.asList("-R", "1000"));
		//classifiers.put("weka.classifiers.functions.SMOreg", null);
		//classifiers.put("weka.classifiers.functions.LinearRegression", null);
		//classifiers.put("weka.classifiers.trees.M5P", null);
		//classifiers.put("weka.classifiers.lazy.IBk", null);
		//classifiers.put("weka.classifiers.trees.REPTree", null);
		//classifiers.put("weka.classifiers.functions.MultilayerPerceptron", null);

		// Constant!
		double avgRateWomen = 0.000237927;
		double avgRateMen = 0.00021608;

		for (String className : classifiers.keySet()) {
			System.out.printf("%s\n", className);
			for (double k : ks) {
				double sumRate = 0.0;
				for (int i = 0; i < runs; i++) {
					DataSplitter splitter = new DataSplitter(data, 0.1);
					Instances estimation = splitter.split();
					Instances training = splitter.remaining();

					estimation.deleteAttributeAt(estimation.attribute("Clicks_Count").index());
					estimation.deleteAttributeAt(estimation.attribute("Impressions").index());
					estimation.insertAttributeAt(new Attribute("ActionRate"), estimation.numAttributes());
					estimation.setClass(estimation.attribute("ActionRate"));

					String[] opts = null;
					if (classifiers.get(className) != null) {
						opts = classifiers.get(className).toArray(new String[0]);
					}
					Estimator est = Estimator.factory(training, className, opts);
					for (Instance inst : estimation) {
						inst.setClassValue(est.estimate(inst));
					}
					EstimationEvaluator evaluator = new EstimationEvaluator(estimation, data);
					evaluator.delta(k*avgRateWomen);
					sumRate += evaluator.meanError();
				}
				//System.out.printf("%.2f: %.8f\n", k, sumRate/runs);
			}
		}

	}
}