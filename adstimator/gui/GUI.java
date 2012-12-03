package adstimator.gui;

import adstimator.core.CombinationAdFactory;
import adstimator.data.DataManager;
import adstimator.data.DatabaseManager;
import adstimator.io.FacebookDataParser;
import adstimator.io.Exporter;
import adstimator.core.AdFactory;
import adstimator.core.Estimator;
import adstimator.data.Ads;
import adstimator.gui.models.AdsTableModel;
import java.awt.Color;
import java.awt.Toolkit;
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
	private AdsTableModel tableModel;
	private DataManager dataManager;
	private AdFactory adFactory;

	// GUI components
	private JTable table;
	private JComboBox cmbGender;
	private JComboBox cmbAge;
	private JComboBox cmbFilterType;
	private JTextField filterText;

	private TableRowSorter<AdsTableModel> sorter;

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

		this.dataManager = new DatabaseManager("instances");

		// Menu bar
		JMenuBar menuBar = new JMenuBar();
		JMenu menuFile = new JMenu("File");
		JMenuItem menuItmReport = new JMenuItem("Add report");
		menuItmReport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(new FileNameExtensionFilter("CSV", "csv"));
				int returnVal = chooser.showOpenDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					dataManager.add(new Ads(FacebookDataParser.parse(chooser.getSelectedFile())));
				}
			}
		});
		menuItmReport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuFile.add(menuItmReport);

		JMenuItem menuItmExport = new JMenuItem("Export selection");
		menuItmExport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
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
				List<Map<String,String>> adList = new LinkedList<Map<String,String>>();
				for (int row : selection) {
					Map<String,String> adMap = new HashMap<String,String>();
					if (campaignName != null) {
						adMap.put("Campaign Name", campaignName);
					}
					adMap.put("Body", tableModel.getValueAt(row, tableModel.findColumn("Body")).toString());
					adMap.put("Image Hash", tableModel.getValueAt(row, tableModel.findColumn("Image_Hash")).toString());
					String val = (String)cmbGender.getSelectedItem();
					if (val.equalsIgnoreCase("All")) {
						val = "";
					}
					adMap.put("Gender", val);
					val = (String)cmbAge.getSelectedItem();
					if (!val.equalsIgnoreCase("All")) {
						adMap.put("Age Min", val.substring(0, val.indexOf("-")));
						adMap.put("Age Max", val.substring(val.indexOf("-")+1));
					}
					adList.add(adMap);
				}
				// Perform export
				Exporter exp = new Exporter("data/export-template.csv");
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(new FileNameExtensionFilter("CSV", "csv"));
				int returnVal = chooser.showSaveDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					exp.export(chooser.getSelectedFile(), adList);
					JOptionPane.showMessageDialog(GUI.this, "All lines were exported successfully.");
				}
			}
		});
		menuItmExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuFile.add(menuItmExport);

		JMenuItem quit = new JMenuItem("Quit");
		quit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		menuFile.add(quit);

		menuBar.add(menuFile);
		this.setJMenuBar(menuBar);

		// Target
		DatabaseLoader loader = null;
		Instances inst = null;
		try {
			loader = new DatabaseLoader();
			loader.connectToDatabase();
			loader.setQuery("SELECT Gender, Age_Min, Age_Max FROM instances");
			inst = loader.getDataSet();
			loader.reset();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		JPanel pnlTarget = new JPanel(new MigLayout("ins 5"));
		pnlTarget.add(new JLabel("Target:"));
		List<String> genders = new LinkedList<String>();
		if (inst != null) {
			for (int i = 0; i < inst.attribute("Gender").numValues(); i++) {
				genders.add(inst.attribute("Gender").value(i));
			}
		}
		genders.add("All");
		cmbGender = new JComboBox(genders.toArray(new String[0]));
		pnlTarget.add(cmbGender);

		Set<String> ages = new TreeSet<String>();
		if (inst != null) {
			Attribute attMinAge = inst.attribute("Age_Min");
			Attribute attMaxAge = inst.attribute("Age_Max");
			for (Instance instance : inst) {
				ages.add(instance.value(attMinAge) + "-" + instance.value(attMaxAge));
			}
		}
		ages.add("All");
		cmbAge = new JComboBox(ages.toArray(new String[0]));
		pnlTarget.add(cmbAge);
		JButton btnSubmit = new JButton("Show suggestions");
		btnSubmit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					dataManager.resetWhere();

					String val = (String)cmbGender.getSelectedItem();
					if (!val.equalsIgnoreCase("All")) {
						dataManager.where("Gender", val);
					}
					val = (String)cmbAge.getSelectedItem();
					if (!val.equalsIgnoreCase("All")) {
						dataManager.where("Age_Min", val.substring(0, val.indexOf("-")));
					}

					Estimator est = Estimator.factory(dataManager.get(), "weka.classifiers.functions.Logistic", Arrays.asList("-R", "1000").toArray(new String[0]));
					adFactory = new CombinationAdFactory(dataManager);
					Instances ads = adFactory.all();
					ads.setClassIndex(ads.numAttributes()-1);
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
		this.add(pnlTarget, "wrap");

		// Table
		this.sorter = new TableRowSorter<AdsTableModel>(this.tableModel);
		table = new JTable(this.tableModel)  {
			public TableCellRenderer getCellRenderer(int row, int column) {
				if (!GUI.this.tableModel.isEstimated(this.convertRowIndexToModel(row))) {
					DefaultTableCellRenderer cr = new DefaultTableCellRenderer();
					cr.setBackground(new Color(100, 100, 100, 20));
					return cr;
				}
				// else...
				return super.getCellRenderer(row, column);
			}
		};
		table.setRowSorter(this.sorter);
		JScrollPane scrollPane = new JScrollPane(table);
		this.add(scrollPane, "grow 100 100, wrap");

		// Filtering
		JPanel pnlFilter = new JPanel(new MigLayout("fillx, ins 5"));
		pnlFilter.add(new JLabel("Filters:"), "gap 0 5, align left");
		this.cmbFilterType = new JComboBox(Arrays.asList("All", "Existing","Suggestions").toArray(new String[0]));
		cmbFilterType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GUI.this.updateFilter();
			}
		});

		pnlFilter.add(this.cmbFilterType, "gap 0 5, align left");
		this.filterText = new JTextField();
		//Whenever filterText changes, invoke newFilter.
		this.filterText.getDocument().addDocumentListener(
			new DocumentListener() {
				public void changedUpdate(DocumentEvent e) {
					GUI.this.updateFilter();
				}
				public void insertUpdate(DocumentEvent e) {
					GUI.this.updateFilter();
				}
				public void removeUpdate(DocumentEvent e) {
					GUI.this.updateFilter();
				}
		});
		pnlFilter.add(this.filterText, "align left, pushx 100, growx 100, gap 0 0");
		this.add(pnlFilter, "growx");
		
		// Pack frame
		this.pack();
	}

	private void updateFilter()
	{
		// Set type filter
		RowFilter<AdsTableModel,Integer> typeFilter = new RowFilter<AdsTableModel,Integer>() {
			public boolean include(Entry<? extends AdsTableModel, ? extends Integer> entry) {
				AdsTableModel model = entry.getModel();
				int row = entry.getIdentifier();
				String selection = (String)cmbFilterType.getSelectedItem();
				boolean showType = true;
				if (selection.equals("Suggestions")) {
					showType = model.isEstimated(row);
				} else if (selection.equals("Existing")) {
					showType = !model.isEstimated(row);
				}
				if (showType) {
					for (int i = 0; i < model.getColumnCount(); i++) {
						String cellValue = (String)model.getValueAt(row, i);
						if (cellValue.indexOf(GUI.this.filterText.getText()) > -1) {
							return true;
						}
					}
				}
				return false;
			}
		};

		// Add filter
		sorter.setRowFilter(typeFilter);
	}

}