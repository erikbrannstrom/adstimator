package adstimator.gui.controllers;

import adstimator.gui.models.SelectionInterface;
import adstimator.gui.models.TargetInterface;
import adstimator.io.Exporter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * This action is used when exporting a selection. The action will ask for a campaign name but otherwise rely on the
 * data from the target and selection interfaces to know what to export. The target data will be repeated for each
 * exported row, where as the selection has its own key-value map for each row.
 * 
 * The file to which the export is written is chosen by the user in a file dialog.
 *
 * @author erikbrannstrom
 */
public class ExportActionListener implements ActionListener
{

	private Exporter exporter;
	private final SelectionInterface selection;
	private final TargetInterface target;

	/**
	 * Initialize a new listener with an exporter as well as a selection and a target interface.
	 * 
	 * @param exporter
	 * @param table
	 * @param target 
	 */
	public ExportActionListener(Exporter exporter, SelectionInterface table, TargetInterface target)
	{
		this.exporter = exporter;
		this.selection = table;
		this.target = target;
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		List<Map<String, String>> adList = this.selection.exportSelected();
		Map<String, String> additions = this.target.currentTarget();
		String campaignName = JOptionPane.showInputDialog(null, "What is the name of the campaign?");
		additions.put("Campaign Name", campaignName);
		
		for (Map<String, String> map : adList) {
			map.putAll(additions);
		}

		if (adList.isEmpty()) {
			JOptionPane.showMessageDialog(null, "No rows selected.");
			return;
		}

		// Perform export
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("CSV", "csv"));
		int returnVal = chooser.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			this.exporter.export(chooser.getSelectedFile(), adList);
			JOptionPane.showMessageDialog(null, "All lines were exported successfully.");
		}
	}
}
