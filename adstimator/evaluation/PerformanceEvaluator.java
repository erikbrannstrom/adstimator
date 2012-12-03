package adstimator.evaluation;

import adstimator.core.Estimator;
import java.util.*;
import weka.core.*;
import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import cern.jet.random.Normal;
import cern.jet.random.engine.RandomEngine;

public class PerformanceEvaluator
{
	private int nominalValues, occurrences;
	private long time;

	public PerformanceEvaluator(int nominalValues, int occurrences)
	{
		this.nominalValues = nominalValues;
		this.occurrences = occurrences;
		this.time = -1;
	}

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
		Attribute clicks = new Attribute("Clicks_Count");
		Attribute impressions = new Attribute("Impressions");
		Attribute actionRate = new Attribute("Action Rate");
		attributes.add(bodyAttr);
		testAttributes.add(bodyAttr);
		attributes.add(imageAttr);
		testAttributes.add(imageAttr);
		attributes.add(clicks);
		attributes.add(impressions);
		testAttributes.add(actionRate);

		Normal ctrDistr = new Normal(0.02, 0.01, RandomEngine.makeDefault());
		Normal impressionDistr = new Normal(150000.0, 30000.0, RandomEngine.makeDefault());
		Instances trainingData = new Instances("Generated Training Data", attributes, nominalValues);
		Instances testData = new Instances("Generated Test Data", testAttributes, nominalValues);
		testData.setClassIndex(testData.numAttributes()-1);
		for (int i = 1; i <= this.nominalValues; i++) {
			String body = "Text-" + i;
			for (int j = 1; j <= this.nominalValues; j++) {
				String image = "Image-" + j;
				if (i != j) {
					for (int occurence = 0; occurence < this.occurrences; occurence++) {
						Instance instance = new DenseInstance(4);
						instance.setDataset(trainingData);
						instance.setValue(bodyAttr, body);
						instance.setValue(imageAttr, image);
						double ctr = Math.abs(ctrDistr.nextDouble());
						int impressionCount = (int)Math.round(impressionDistr.nextDouble(150000.0, 30000.0));
						instance.setValue(impressions, impressionCount+1);
						instance.setValue(clicks, (int)Math.round(ctr*impressionCount)+1); // Cannot be 0
						trainingData.add(instance);
					}
				} else {
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
		Estimator est = Estimator.factory(trainingData, "weka.classifiers.functions.Logistic", Arrays.asList("-R", "1000").toArray(new String[0]));
		for (Instance testInstance : testData) {
			testInstance.setClassValue(est.estimate(testInstance));
		}

		// Return time taken to build classifier
		this.time = System.currentTimeMillis() - start;
	}

	public long time()
	{
		if (this.time == -1) {
			this.evaluate();
		}
		return this.time;
	}

	public String result()
	{
		if (this.time == -1) {
			this.evaluate();
		}
		return String.format("Classifier was built in %.4f seconds.", this.time/1000.0);
	}

	public String description()
	{
		return "Evaluate the performance of an estimator by building the classifier from a large generated data set.";
	}

	public static void main(String[] args) throws Exception
	{
		for (int i = 10; i <= 100; i = i+10) {
			long sumTime = 0;
			for (int trial = 1; trial <= 3; trial++) {
				PerformanceEvaluator evaluator = new PerformanceEvaluator(10, i);
				sumTime += evaluator.time();
			}
			System.out.println(i + ": " + sumTime/3.0);
		}
	}

}