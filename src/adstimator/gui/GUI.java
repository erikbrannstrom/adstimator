package adstimator.gui;

import adstimator.core.AdFactory;
import adstimator.core.CombinationAdFactory;
import adstimator.core.Estimator;
import adstimator.data.Ads;
import adstimator.data.DatabaseManager;
import adstimator.data.KnowledgeBase;
import adstimator.data.KnowledgeBaseContainer;
import adstimator.gui.models.AdsTableModel;
import adstimator.gui.views.AdsTable;
import adstimator.gui.views.FilterPanel;
import adstimator.gui.views.TargetPanel;
import adstimator.io.Exporter;
import adstimator.io.FacebookDataParser;
import adstimator.utils.Config;
import java.awt.Toolkit;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.miginfocom.swing.MigLayout;
import weka.core.*;

public class GUI extends JFrame implements Observer
{
	private Config config;
	private AdsTableModel tableModel;
	private DatabaseManager dataManager;
	private AdFactory adFactory;
	// GUI components
	private AdsTable table;
	private TargetPanel targetPanel;
	private JMenu menuDatabase;
	private KnowledgeBaseContainer kbContainer;

	public GUI()
	{
		super("Ad Estimator");
		if (System.getProperty("os.name").contains("Mac")) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
		this.tableModel = new AdsTableModel(null, null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		JPanel panel = new JPanel(new MigLayout("fill", "", "[]10:10:10[grow]10:10:10[]"));
		panel.setOpaque(true);
		this.setContentPane(panel);

		this.config = new Config();
		
		this.kbContainer = new KnowledgeBaseContainer(Integer.parseInt(this.config.get("knowledge_base")));
		this.targetPanel = new TargetPanel(this.kbContainer);
		this.kbContainer.addObserver(this);
		this.update(this.kbContainer, null);

		// Menu bar
		JMenuBar menuBar = new JMenuBar();
		JMenu menuFile = new JMenu("File");
		JMenuItem menuItmReport = new JMenuItem("Import report");
		menuItmReport.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(new FileNameExtensionFilter("CSV", "csv"));
				int returnVal = chooser.showOpenDialog(GUI.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					dataManager.add(new Ads(FacebookDataParser.parse(chooser.getSelectedFile())));
					kbContainer.notifyObservers();
				}
			}
		});
		menuItmReport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuFile.add(menuItmReport);

		JMenuItem menuItmExport = new JMenuItem("Export selection");
		menuItmExport.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Map<String, String> target = new HashMap<String, String>();
				// Ask for campaign name
				String campaignName = JOptionPane.showInputDialog(GUI.this, "What is the name of the campaign?");
				target.put("Campaign Name", campaignName);
				target.putAll(targetPanel.currentTarget());
				
				List<Map<String, String>> adList = GUI.this.table.exportSelectedRows(target);
				if (adList == null) {
					JOptionPane.showMessageDialog(GUI.this, "No rows selected.");
				}
				// Perform export
				Exporter exp = new Exporter("resources/export-template.csv");
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(new FileNameExtensionFilter("CSV", "csv"));
				int returnVal = chooser.showSaveDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					exp.export(chooser.getSelectedFile(), adList);
					JOptionPane.showMessageDialog(GUI.this, "All lines were exported successfully.");
				}
			}
		});
		menuItmExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuFile.add(menuItmExport);

		JMenuItem quit = new JMenuItem("Quit");
		quit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		});
		menuFile.add(quit);

		menuDatabase = new JMenu("Knowledge bases");
		this.updateKBMenu();

		menuBar.add(menuFile);
		menuBar.add(menuDatabase);
		this.setJMenuBar(menuBar);

		///////////////
		// Targeting //
		///////////////
		JPanel pnlTarget = new JPanel(new MigLayout("ins 5"));
		pnlTarget.add(this.targetPanel);

		// Buttons for choosing what the data table should show (suggestions, texts or images)
		JButton btnSubmit = new JButton("Show suggestions");
		btnSubmit.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				GUI.this.updateWhere();
				try {
					//Estimator est = Estimator.factory(dataManager.get(), "weka.classifiers.functions.Logistic", 
					//		Arrays.asList("-R", "1000").toArray(new String[0]));
					Estimator est = Estimator.factory(dataManager.get(), "weka.classifiers.lazy.IBk", null);
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
			public void actionPerformed(ActionEvent e)
			{
				GUI.this.updateWhere();

				Ads knowledge = dataManager.getAggregate("Body");
				knowledge.convertToRate();
				tableModel.setData(null, knowledge);
			}
		});
		pnlTarget.add(btnTexts);
		JButton btnImages = new JButton("Show images");
		btnImages.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				GUI.this.updateWhere();

				Ads knowledge = dataManager.getAggregate("Image_Hash");
				knowledge.convertToRate();

				tableModel.setData(null, knowledge);
			}
		});
		pnlTarget.add(btnImages);
		this.add(pnlTarget, "wrap");

		// Table
		this.table = new AdsTable(this.tableModel);

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

	private void updateKBMenu()
	{
		this.menuDatabase.removeAll();

		JMenuItem menuItmNew = new JMenuItem("New KB");
		menuItmNew.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				String name = JOptionPane.showInputDialog(GUI.this, "Name the new knowledge base:");
				if (name != null && name.length() > 0) {
					KnowledgeBase kb = new KnowledgeBase(name);
					kb.save();
					GUI.this.updateKBMenu();
				}
			}
		});
		this.menuDatabase.add(menuItmNew);

		JMenuItem menuItmDelete = new JMenuItem("Delete current KB");
		menuItmDelete.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				int answer = JOptionPane.showConfirmDialog(GUI.this,
						String.format("Are you sure you want to delete the knowledge base %s?", GUI.this.kbContainer.getKnowledgeBase().name()),
						"Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (answer == JOptionPane.YES_OPTION) {
					List<KnowledgeBase> kbs = KnowledgeBase.getAll();
					if (kbs.size() > 1) {
						GUI.this.kbContainer.getKnowledgeBase().delete();
						kbs = KnowledgeBase.getAll();
						GUI.this.kbContainer.setKnowledgeBase(kbs.get(0));
						GUI.this.updateKBMenu();
						GUI.this.tableModel.setData(null, null);
					} else {
						JOptionPane.showMessageDialog(GUI.this, "Cannot remove the last KB. Create a new one first.");
					}
				}
			}
		});
		this.menuDatabase.add(menuItmDelete);

		this.menuDatabase.addSeparator();

		ButtonGroup group = new ButtonGroup();
		int activeKB = Integer.parseInt(this.config.get("knowledge_base"));
		for (final KnowledgeBase kb : KnowledgeBase.getAll()) {
			JRadioButtonMenuItem itm = new JRadioButtonMenuItem(kb.name(), kb.id() == activeKB);
			itm.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					GUI.this.kbContainer.setKnowledgeBase(kb);
					GUI.this.tableModel.setData(null, null);
				}
			});
			group.add(itm);
			this.menuDatabase.add(itm);
		}
	}

	@Override
	public void update(Observable o, Object o1)
	{
		GUI.this.config.set("knowledge_base", String.valueOf(this.kbContainer.getKnowledgeBase().id()));
		this.dataManager = new DatabaseManager(this.kbContainer.getKnowledgeBase().table());
	}
	
}