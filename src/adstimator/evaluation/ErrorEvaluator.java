package adstimator.evaluation;

import adstimator.data.Ads;
import weka.core.*;

/**
 * Class for evaluating the mean squared error.
 * 
 * The evaluator expects two sets of data that should include the same set of instances, but where the first one
 * should have been run through an estimator. The difference between identical (as per the findMatch method of Ads)
 * is squared and the average for all instances is output in the results.
 * 
 * @author erikbrannstrom
 */
public class ErrorEvaluator extends Evaluator
{
	private Ads estimated, validation;

	/**
	 * Create a new error evaluator. The validation set will be converted to rate instead if it contains the separate
	 * metrics.
	 * 
	 * @param estimated Set of ads which has been estimated using an Estimator
	 * @param validation Same data set, but with real values
	 */
	public ErrorEvaluator(Ads estimated, Ads validation)
	{
		this.estimated = estimated;
		this.validation = validation;
		this.validation.convertToRate();
	}

	/**
	 * Calculate the mean squared error between two sets of data, which are expected to contain the same instances, but
	 * with one having estimated click rate.
	 * 
	 * @return Mean squared error
	 */
	public double meanSquaredError()
	{
		int count = 0;
		double meanError = 0.0;

		for (Instance suggestion : this.estimated) {
			count++;
			double estimate = suggestion.classValue();
			Instance match = this.validation.findMatch(suggestion);
			double real = match.classValue();

			meanError += (estimate - real)*(estimate - real);
		}

		return meanError/count;
	}

	/**
	 * Return string containing the value of the mean squared error.
	 * 
	 * @return 
	 */
	@Override
	public String result()
	{
		return String.format("Mean squared error is %.6f for data set.", this.meanSquaredError());
	}

	/**
	 * Textual description of evaluator.
	 * 
	 * @return 
	 */
	@Override
	public String description()
	{
		return "Evaluate the mean squared error of instances in two data sets.";
	}

}