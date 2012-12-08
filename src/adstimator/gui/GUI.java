package adstimator.gui;

import adstimator.core.AdFactory;
import adstimator.core.CombinationAdFactory;
import adstimator.core.Estimator;
import adstimator.data.Ads;
import adstimator.data.DatabaseManager;
import adstimator.gui.models.AdsTableModel;
import adstimator.io.Exporter;
import adstimator.io.FacebookDataParser;
import adstimator.utils.Config;
import adstimator.utils.KnowledgeBase;
import java.awt.Color;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import net.miginfocom.swing.MigLayout;
import weka.core.*;
import weka.core.converters.DatabaseLoader;

public class GUI extends JFrame
{

	private Config config;
	private KnowledgeBase currentKb;
	private AdsTableModel tableModel;
	private DatabaseManager dataManager;
	private AdFactory adFactory;
	// GUI components
	private JTable table;
	private JComboBox cmbGender;
	private JComboBox cmbAge;
	private JComboBox cmbFilterType;
	private JTextField filterText;
	private JMenu menuDatabase;
	private TableRowSorter<AdsTableModel> sorter;
	private List<? extends RowSorter.SortKey> lastSortFull, lastSortRank;
	private boolean fullLastActive;

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

		cmbGender = new JComboBox();
		cmbAge = new JComboBox();

		this.config = new Config();

		this.setCurrentKB(Integer.parseInt(this.config.get("knowledge_base")));

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
					GUI.this.updateTargetControls();
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
				// Get selected rows
				int[] selection = table.getSelectedRows();
				for (int i = 0; i < selection.length; i++) {
					selection[i] = table.convertRowIndexToModel(selection[i]);
				}
				if (selection.length == 0) {
					JOptionPane.showMessageDialog(GUI.this, "No rows selected.");
					return;
				}
				// Ask for campaign name
				String campaignName = JOptionPane.showInputDialog(GUI.this, "What is the name of the campaign?");
				// Create list with a map for each ad
				List<Map<String, String>> adList = new LinkedList<Map<String, String>>();
				for (int row : selection) {
					Map<String, String> adMap = new HashMap<String, String>();
					if (campaignName != null) {
						adMap.put("Campaign Name", campaignName);
					}
					adMap.put("Body", tableModel.getValueAt(row, tableModel.findColumn("Body")).toString());
					adMap.put("Image Hash", tableModel.getValueAt(row, tableModel.findColumn("Image_Hash")).toString());
					String val = (String) cmbGender.getSelectedItem();
					if (val.equalsIgnoreCase("All")) {
						val = "";
					}
					adMap.put("Gender", val);
					val = (String) cmbAge.getSelectedItem();
					if (!val.equalsIgnoreCase("All")) {
						adMap.put("Age Min", val.substring(0, val.indexOf("-")));
						adMap.put("Age Max", val.substring(val.indexOf("-") + 1));
					}
					adList.add(adMap);
				}
				// Perform export
				Exporter exp = new Exporter("data/export-template.csv");
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
		pnlTarget.add(new JLabel("Target:"));
		pnlTarget.add(cmbGender);
		pnlTarget.add(cmbAge);

		this.updateTargetControls();

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

					GUI.this.storeSortKeys(true);
					tableModel.setData(new Ads(ads), knowledge);
					GUI.this.sorter.setSortKeys(GUI.this.getSortKeys(true));
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
				GUI.this.storeSortKeys(false);
				tableModel.setData(null, knowledge);
				GUI.this.sorter.setSortKeys(GUI.this.getSortKeys(false));
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
				
				GUI.this.storeSortKeys(false);
				tableModel.setData(null, knowledge);
				GUI.this.sorter.setSortKeys(GUI.this.getSortKeys(false));
			}
		});
		pnlTarget.add(btnImages);
		this.add(pnlTarget, "wrap");

		// Table
		this.sorter = new TableRowSorter<AdsTableModel>(this.tableModel);
		table = new JTable(this.tableModel)
		{
			public TableCellRenderer getCellRenderer(int row, int column)
			{
				if (!GUI.this.tableModel.isEstimated(this.convertRowIndexToModel(row))) {
					DefaultTableCellRenderer cr = new DefaultTableCellRenderer();
					cr.setBackground(new Color(100, 100, 100, 20));
					return cr;
				} else {
					DefaultTableCellRenderer cr = new DefaultTableCellRenderer();
					cr.setBackground(new Color(240, 190, 110, 80));
					return cr;
				}
			}
		};
		table.setRowSorter(this.sorter);

		table.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				int r = table.rowAtPoint(e.getPoint());
				if (r >= 0 && r < table.getRowCount()) {
					table.setRowSelectionInterval(r, r);
				} else {
					table.clearSelection();
				}

				int rowindex = table.getSelectedRow();
				if (rowindex < 0) {
					return;
				}
				if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
					JPopupMenu popup = createYourPopUp(e.getPoint());
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}

			private JPopupMenu createYourPopUp(final Point p)
			{
				JPopupMenu menu = new JPopupMenu();
				JMenuItem itm = new JMenuItem("Copy cell value");
				itm.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent ae)
					{
						int row = GUI.this.table.rowAtPoint(p);
						int column = GUI.this.table.columnAtPoint(p);
						
						Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
						StringSelection data = new StringSelection(GUI.this.table.getValueAt(row, column).toString());
						c.setContents(data, data);
					}
				});
				menu.add(itm);
				return menu;
			}
		});
		
		JScrollPane scrollPane = new JScrollPane(table);
		this.add(scrollPane, "grow 100 100, wrap");

		// Filtering
		JPanel pnlFilter = new JPanel(new MigLayout("fillx, ins 5"));
		pnlFilter.add(new JLabel("Filters:"), "gap 0 5, align left");
		this.cmbFilterType = new JComboBox(Arrays.asList("All", "Existing", "Suggestions").toArray(new String[0]));
		cmbFilterType.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				GUI.this.updateFilter();
			}
		});

		pnlFilter.add(this.cmbFilterType, "gap 0 5, align left");
		this.filterText = new JTextField();
		// Whenever filterText changes, update the filter on the table.
		this.filterText.getDocument().addDocumentListener(
				new DocumentListener()
				{
					public void changedUpdate(DocumentEvent e)
					{
						GUI.this.updateFilter();
					}

					public void insertUpdate(DocumentEvent e)
					{
						GUI.this.updateFilter();
					}

					public void removeUpdate(DocumentEvent e)
					{
						GUI.this.updateFilter();
					}
				});
		pnlFilter.add(this.filterText, "align left, pushx 100, growx 100, gap 0 0");
		this.add(pnlFilter, "growx");

		// Pack frame
		this.pack();
	}

	/**
	 * Private method which should be called when a new report has been imported in order to repopulate the targeting
	 * options.
	 */
	private void updateTargetControls()
	{
		Instances inst = null;
		try {
			DatabaseLoader loader = new DatabaseLoader();
			loader.connectToDatabase();
			loader.setQuery("SELECT Gender, Age_Min, Age_Max FROM " + this.currentKb.table());
			inst = loader.getDataSet();
			loader.reset();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		List<String> genders = new LinkedList<String>();
		if (inst != null) {
			for (int i = 0; i < inst.attribute("Gender").numValues(); i++) {
				genders.add(inst.attribute("Gender").value(i));
			}
		}
		genders.add("All");
		this.cmbGender.removeAllItems();
		for (String gender : genders) {
			this.cmbGender.addItem(gender);
		}

		Set<String> ages = new TreeSet<String>();
		if (inst != null) {
			Attribute attMinAge = inst.attribute("Age_Min");
			Attribute attMaxAge = inst.attribute("Age_Max");
			for (Instance instance : inst) {
				ages.add(instance.value(attMinAge) + "-" + instance.value(attMaxAge));
			}
		}
		ages.add("All");
		this.cmbAge.removeAllItems();
		for (String age : ages) {
			this.cmbAge.addItem(age);
		}
	}

	private void storeSortKeys(boolean fullTable)
	{
		// Only store sorting if there is data to be sorted
		if (this.tableModel.getColumnCount() > 0) {
			if (this.fullLastActive) {
				this.lastSortFull = this.table.getRowSorter().getSortKeys();
			} else {
				this.lastSortRank = this.table.getRowSorter().getSortKeys();
			}
		}
		
		this.fullLastActive = fullTable;
	}
	
	private List<? extends RowSorter.SortKey> getSortKeys(boolean fullTable)
	{
		if ( (fullTable && this.lastSortFull == null) || (!fullTable && this.lastSortRank == null) ) {
			// Default sort keys are returned if we have no previous sorting
			return Arrays.asList(new RowSorter.SortKey(tableModel.getColumnCount() - 1, SortOrder.DESCENDING));
		} else if (fullTable) {
			// Last used sorting for full table
			return this.lastSortFull;
		} else {
			// Last used sorting for rank table
			return this.lastSortRank;
		}
	}

	/**
	 * Private method which should be called whenever the targeting options are changed.
	 */
	private void updateWhere()
	{
		// Reset where clauses
		this.dataManager.resetWhere();

		// Set gender, if other than all is selected
		String val = (String) cmbGender.getSelectedItem();
		if (!val.equalsIgnoreCase("All")) {
			this.dataManager.where("Gender", val);
		}

		// Set ages, if other than all is selected
		val = (String) cmbAge.getSelectedItem();
		if (!val.equalsIgnoreCase("All")) {
			this.dataManager.where("Age_Min", val.substring(0, val.indexOf("-")));
			this.dataManager.where("Age_Max", val.substring(val.indexOf("-") + 1));
		}
	}

	/**
	 * Private method which should be called whenever any of the filter options are updated.
	 */
	private void updateFilter()
	{
		// Set type filter
		RowFilter<AdsTableModel, Integer> typeFilter = new RowFilter<AdsTableModel, Integer>()
		{
			public boolean include(Entry<? extends AdsTableModel, ? extends Integer> entry)
			{
				AdsTableModel model = entry.getModel();
				int row = entry.getIdentifier();

				// Should the type of ad (suggestion, existing or all) be shown?
				String selection = (String) cmbFilterType.getSelectedItem();
				boolean showType = true;
				if (selection.equals("Suggestions")) {
					showType = model.isEstimated(row);
				} else if (selection.equals("Existing")) {
					showType = !model.isEstimated(row);
				}

				// If the type matches, perform string matching to reach final decision
				if (showType) {
					for (int i = 0; i < model.getColumnCount(); i++) {
						String cellValue = (String) model.getValueAt(row, i);
						if (cellValue.toLowerCase().indexOf(GUI.this.filterText.getText().toLowerCase()) > -1) {
							// String exists in at least one cell, so the row should be shown
							return true;
						}
					}
				}

				// Either the row is of the wrong type, or the text could not be found in any cell
				return false;
			}
		};

		// Apply filter
		this.sorter.setRowFilter(typeFilter);
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
						String.format("Are you sure you want to delete the knowledge base %s?", GUI.this.currentKb.name()), 
						"Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (answer == JOptionPane.YES_OPTION) {
					List<KnowledgeBase> kbs = KnowledgeBase.getAll();
					if (kbs.size() > 1) {
						GUI.this.currentKb.delete();
						kbs = KnowledgeBase.getAll();
						GUI.this.setCurrentKB(kbs.get(0));
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
					GUI.this.setCurrentKB(kb);
					GUI.this.tableModel.setData(null, null);
				}
			});
			group.add(itm);
			this.menuDatabase.add(itm);
		}
	}
	
	private void setCurrentKB(KnowledgeBase kb)
	{
		if (this.currentKb != null && this.currentKb.id() == kb.id()) {
			return;
		}
		
		GUI.this.config.set("knowledge_base", String.valueOf(kb.id()));
		this.currentKb = kb;
		this.dataManager = new DatabaseManager(kb.table());
		this.updateTargetControls();
	}

	private void setCurrentKB(int id)
	{
		this.setCurrentKB(KnowledgeBase.find(id));
	}
}