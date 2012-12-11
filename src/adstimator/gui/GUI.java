package adstimator.gui;

import adstimator.data.*;
import adstimator.gui.controllers.DataActionListener;
import adstimator.gui.controllers.ExportActionListener;
import adstimator.gui.models.AdsTableModel;
import adstimator.gui.views.*;
import adstimator.io.Exporter;
import adstimator.utils.Config;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import net.miginfocom.swing.MigLayout;

public class GUI extends JFrame implements Observer
{

	private Config config;
	private AdsTableModel tableModel;
	private KnowledgeBaseContainer kbContainer;
	// GUI components
	private AdsTable table;
	private TargetPanel targetPanel;

	public GUI()
	{
		// Initialize JFrame and its settings
		super("Ad Estimator");
		if (System.getProperty("os.name").contains("Mac")) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		// Create a main content pane with MigLayout
		JPanel panel = new JPanel(new MigLayout("fill", "", "[]10:10:10[grow]10:10:10[]"));
		panel.setOpaque(true);
		this.setContentPane(panel);
		
		// Initialize private variables
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
		ActionListener showAction = new DataActionListener(kbContainer, targetPanel, tableModel);
		
		JButton btnSubmit = new JButton("Show suggestions");
		btnSubmit.setActionCommand("All");
		btnSubmit.addActionListener(showAction);
		pnlTarget.add(btnSubmit);
		
		JButton btnTexts = new JButton("Show texts");
		btnTexts.setActionCommand("Body");
		btnTexts.addActionListener(showAction);
		pnlTarget.add(btnTexts);
		
		JButton btnImages = new JButton("Show images");
		btnImages.setActionCommand("Image Hash");
		btnImages.addActionListener(showAction);
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

	@Override
	public final void update(Observable o, Object o1)
	{
		GUI.this.config.set("knowledge_base", String.valueOf(this.kbContainer.getKnowledgeBase().id()));
		this.tableModel.setData(null, null);
	}
}