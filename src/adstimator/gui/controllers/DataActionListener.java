package adstimator.gui.controllers;

import adstimator.core.AdFactory;
import adstimator.core.CombinationAdFactory;
import adstimator.core.Estimator;
import adstimator.data.Ads;
import adstimator.data.KnowledgeBaseContainer;
import adstimator.gui.models.AdsTableModel;
import adstimator.gui.views.TargetPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import javax.swing.JOptionPane;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author erikbrannstrom
 */
public class DataActionListener implements ActionListener
{
	private KnowledgeBaseContainer kbContainer;
	private TargetPanel targetPanel;
	private AdsTableModel tableModel;

	public DataActionListener(KnowledgeBaseContainer kbContainer, TargetPanel targetPanel, AdsTableModel tableModel)
	{
		this.kbContainer = kbContainer;
		this.targetPanel = targetPanel;
		this.tableModel = tableModel;
	}
	
	@Override
	public void actionPerformed(ActionEvent ae)
	{
		Map<String, String> currentTarget = this.targetPanel.currentTarget();
		
		if (ae.getActionCommand().equalsIgnoreCase("All")) {
			//Estimator est = Estimator.factory(dataManager.get(), "weka.classifiers.functions.Logistic", 
			//		Arrays.asList("-R", "1000").toArray(new String[0]));
			Ads training = this.kbContainer.getKnowledgeBase().getAds(currentTarget);
			AdFactory adFactory = new CombinationAdFactory(training);
			if (training == null) {
				JOptionPane.showMessageDialog(null, "No data in this knowledge base or for this target.");
				return;
			}
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
