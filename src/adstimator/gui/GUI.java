package adstimator.gui;

import adstimator.core.AdFactory;
import adstimator.core.CombinationAdFactory;
import adstimator.core.Estimator;
import adstimator.data.*;
import adstimator.gui.controllers.ExportActionListener;
import adstimator.gui.models.AdsTableModel;
import adstimator.gui.views.*;
import adstimator.io.Exporter;
import adstimator.utils.Config;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;
import weka.core.*;

public class GUI extends JFrame implements Observer
{

	private Config config;
	private AdsTableModel tableModel;
	private DatabaseManager dataManager;
	private AdFactory adFactory;
	private KnowledgeBaseContainer kbContainer;
	// GUI components
	private AdsTable table;
	private TargetPanel targetPanel;

	public GUI()
	{
		super("Ad Estimator");
		
		// Set correct menubar location on Mac
		if (System.getProperty("os.name").contains("Mac")) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		JPanel panel = new JPanel(new MigLayout("fill", "", "[]10:10:10[grow]10:10:10[]"));
		panel.setOpaque(true);
		this.setContentPane(panel);
		
		this.tableModel = new AdsTableModel(null, null);
		this.table = new AdsTable(this.tableModel);
		this.config = new Config();

		this.kbContainer = new KnowledgeBaseContainer(Integer.parseInt(this.config.get("knowledge_base")));
		this.targetPanel = new TargetPanel(this.kbContainer);
		this.kbContainer.addObserver(this);
		this.update(this.kbContainer, null);

		// Menu bar
		Exporter exp = new Exporter("resources/export-template.csv");
		JMenuBar menuBar = new Menu(new ExportActionListener(exp, this.table, this.targetPanel), this.kbContainer);
		this.setJMenuBar(menuBar);

		// Targeting
		JPanel pnlTarget = new JPanel(new MigLayout("ins 5"));
		pnlTarget.add(this.targetPanel);

		// Buttons for choosing what the data table should show (suggestions, texts or images)
		JButton btnSubmit = new JButton("Show suggestions");
		btnSubmit.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				GUI.this.updateWhere();
				try {
					//Estimator est = Estimator.factory(dataManager.get(), "weka.classifiers.functions.Logistic", 
					//		Arrays.asList("-R", "1000").toArray(new String[0]));
					Ads training = dataManager.get();
					if (training == null) {
						JOptionPane.showMessageDialog(GUI.this, "No data in this knowledge base.");
						return;
					}
					Estimator est = Estimator.factory(training, "weka.classifiers.lazy.IBk", null);
					adFactory = new CombinationAdFactory(dataManager);
					Instances ads = adFactory.all();
					ads.setClassIndex(ads.numAttributes() - 1);
					for (Instance ad : ads) {
						ad.setClassValue(est.estimate(ad));
					}
					Ads knowledge = dataManager.get();
					knowledge.convertToRate();

					tableModel.setData(new Ads(ads), knowledge);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		});

		pnlTarget.add(btnSubmit);
		JButton btnTexts = new JButton("Show texts");
		btnTexts.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				GUI.this.updateWhere();

				Ads knowledge = dataManager.getAggregate("Body");
				if (knowledge == null) {
					JOptionPane.showMessageDialog(GUI.this, "No data in this knowledge base.");
					return;
				}
				knowledge.convertToRate();
				tableModel.setData(null, knowledge);
			}
		});
		pnlTarget.add(btnTexts);
		JButton btnImages = new JButton("Show images");
		btnImages.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				GUI.this.updateWhere();

				Ads knowledge = dataManager.getAggregate("Image_Hash");
				if (knowledge == null) {
					JOptionPane.showMessageDialog(GUI.this, "No data in this knowledge base.");
					return;
				}
				knowledge.convertToRate();

				tableModel.setData(null, knowledge);
			}
		});
		pnlTarget.add(btnImages);
		this.add(pnlTarget, "wrap");

		// Table
		JScrollPane scrollPane = new JScrollPane(table);
		this.add(scrollPane, "grow 100 100, wrap");

		// Filtering
		this.add(new FilterPanel(this.table), "growx");

		// Pack frame
		this.pack();
	}

	/**
	 * Private method which should be called whenever the targeting options are changed.
	 */
	private void updateWhere()
	{
		// Reset where clauses
		this.dataManager.resetWhere();
		Map<String, String> currentTarget = this.targetPanel.currentTarget();

		for (String key : currentTarget.keySet()) {
			this.dataManager.where(key, currentTarget.get(key));
		}
	}

	@Override
	public final void update(Observable o, Object o1)
	{
		GUI.this.config.set("knowledge_base", String.valueOf(this.kbContainer.getKnowledgeBase().id()));
		this.dataManager = new DatabaseManager(this.kbContainer.getKnowledgeBase().table());
		this.tableModel.setData(null, null);
	}
}