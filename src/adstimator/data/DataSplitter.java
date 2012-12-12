package adstimator.data;

import java.util.*;
import weka.core.*;

/**
 * Class for splitting data sets in two.
 * 
 * The ratio is defined by the user, however the ads that are put in each set are chosen randomly.
 * 
 * @author erikbrannstrom
 */
public class DataSplitter
{
	private Instances original, split, remaining;
	private double ratio;

	/**
	 * A DataSplitter will split the input data into two non-overlapping sets by randomly moving instances from the
	 * original set to a new set. The number of instances to be moved is specified by the ratio.
	 *
	 * @param data Data set to be split
	 * @param ratio Real number between 0 and 1 (inclusive)
	 */
	public DataSplitter(Instances data, double ratio)
	{
		if (ratio < 0 || ratio > 1) {
			throw new RuntimeException("Ratio must be a value between 0 and 1.");
		}

		this.original = data;
		this.ratio = ratio;
	}

	/**
	 * Get the set of instances which are not in the split set.
	 * The set contains (1-ratio)*data.numInstances() instances, with the number being rounded down to the nearest
	 * integer.
	 * 
	 * @return Instances Split set
	 */
	public Instances remaining()
	{
		if (this.split == null) {
			this.performSplit();
		}
		return this.remaining;
	}

	/**
	 * Get the set of instances which was randomly selected from the original data set.
	 * The set contains ratio*data.numInstances() instances, with the number being rounded up to the nearest integer.
	 * 
	 * @return Instances Split set
	 */
	public Instances split()
	{
		if (this.split == null) {
			this.performSplit();
		}
		return this.split;
	}

	/**
	 * Private helper method for doing the actual splitting and storing these in the instance.
	 */
	private void performSplit()
	{
		this.remaining = new Instances(this.original);
		this.split = new Instances(this.original, 0, 0);

		Random rnd = new Random();
		int validationSize = (int)Math.ceil(this.remaining.numInstances()*this.ratio);
		for (int i = 0; i < validationSize; i++) {
			int index = Math.abs(rnd.nextInt()) % this.remaining.numInstances();
			Instance inst = this.remaining.get(index);
			inst.setDataset(this.split);
			this.split.add(inst);
			this.remaining.remove(index);
		}
	}

}