package adstimator.experiments;

import adstimator.data.Ads;
import java.util.*;
import weka.core.*;

public class RankTest
{
	Instances suggestions, validation;
	double stdError;

	public RankTest(Instances suggestions, Instances validation)
	{
		this.suggestions = suggestions;
		this.validation = validation;
	}

	public int[] rankMatches()
	{
		int[] results = new int[this.suggestions.numInstances()];

		Instances realRanking = new Instances(this.validation, 0, 0);
		for (int i = 0; i < this.suggestions.numInstances(); i++) {
			Instance suggestion = this.suggestions.get(i);
			Instance real = ((Ads)this.validation).findMatch(suggestion);
			realRanking.add(real);
		}
		this.sortInstances(realRanking);

		this.sortInstances(this.suggestions);
		for (int i = 0; i < results.length; i++) {
			Instance suggestion = this.suggestions.get(i);
			Instance real = ((Ads)realRanking).findMatch(suggestion);

			if (realRanking.indexOf(real) == i) {
				results[i] = 1;
			} else {
				results[i] = 0;
			}
		}

		return results;
	}

	private void sortInstances(Instances instances)
	{
		Comparator<Instance> rateComparator = new Comparator<Instance>() {
			public int compare(Instance a, Instance b) {
				Attribute ctrMean = a.dataset().attribute("CTR");

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

}