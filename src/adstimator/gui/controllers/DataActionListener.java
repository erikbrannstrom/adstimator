package adstimator.gui.controllers;

import adstimator.core.AdFactory;
import adstimator.core.CombinationAdFactory;
import adstimator.core.Estimator;
import adstimator.data.Ads;
import adstimator.data.KnowledgeBaseContainer;
import adstimator.gui.models.AdsTableModel;
import adstimator.gui.models.TargetInterface;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import javax.swing.JOptionPane;
import weka.core.Instance;
import weka.core.Instances;

/**
 * The DataActionListener is central to the whole application, since it is the action listener used when doing
 * estimations. The action consists of getting the type of data to be fetched (full or aggregated) and what the
 * targeting options are, after which an estimator is created based on the relevant data in the knowledge base.
 *
 * Lastly the data from the knowledge base along with the new estimated ads are forwarded to the table model, which
 * causes the table to be updated.
 *
 * The classifier used is Weka's implementation of IBk (a nearest neighbor algorithm). It has only slightly less
 * accurate estimates than logistic regression, while still being a lot faster.
 *
 * @author erikbrannstrom
 */
public class DataActionListener implements ActionListener
{

	private KnowledgeBaseContainer kbContainer;
	private TargetInterface targetPanel;
	private AdsTableModel tableModel;

	/**
	 * Initialize a new listener with a knowledge base container, a target interface and a table model.
	 *
	 * @param kbContainer
	 * @param targeting
	 * @param tableModel
	 */
	public DataActionListener(KnowledgeBaseContainer kbContainer, TargetInterface targeting, AdsTableModel tableModel)
	{
		this.kbContainer = kbContainer;
		this.targetPanel = targeting;
		this.tableModel = tableModel;
	}

	/**
	 * This action will get data from the knowledge base and create an estimator to show the data the user has chosen.
	 * See the class description for more details.
	 *
	 * @param ae
	 */
	@Override
	public void actionPerformed(ActionEvent ae)
	{
		Map<String, String> currentTarget = this.targetPanel.currentTarget();

		if (ae.getActionCommand().equalsIgnoreCase("All")) {
			// Get a full data set with estimates
			Ads training = this.kbContainer.getKnowledgeBase().getAds(currentTarget);
			if (training == null) {
				JOptionPane.showMessageDialog(null, "No data in this knowledge base or for this target.");
				return;
			}
			AdFactory adFactory = new CombinationAdFactory(training);
			// Below is the logistic regression. I have left this comment here since it is not obvious that the option
			// is required for good results.
			//Estimator est = Estimator.factory(dataManager.get(), "weka.classifiers.functions.Logistic", 
			//		Arrays.asList("-R", "1000").toArray(new String[0])); 
			Estimator est = Estimator.factory(training, "weka.classifiers.lazy.IBk", null);
			Instances ads = adFactory.all();
			ads.setClassIndex(ads.numAttributes() - 1);
			for (Instance ad : ads) {
				ad.setClassValue(est.estimate(ad));
			}
			Ads knowledge = this.kbContainer.getKnowledgeBase().getAds(currentTarget);
			knowledge.convertToRate();

			this.tableModel.setData(new Ads(ads), knowledge);
		} else {
			// Get aggregated data
			Ads knowledge = this.kbContainer.getKnowledgeBase().getAggregatedAds(currentTarget, ae.getActionCommand());
			if (knowledge == null) {
				JOptionPane.showMessageDialog(null, "No data in this knowledge base or for this target.");
				return;
			}
			knowledge.convertToRate();

			this.tableModel.setData(null, knowledge);
		}
	}
}
