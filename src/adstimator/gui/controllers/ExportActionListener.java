/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package adstimator.gui.controllers;

import adstimator.gui.models.TargetInterface;
import adstimator.gui.views.AdsTable;
import adstimator.io.Exporter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author erikbrannstrom
 */
public class ExportActionListener implements ActionListener
{

	private Exporter exporter;
	private final AdsTable table;
	private final TargetInterface target;

	public ExportActionListener(Exporter exporter, AdsTable table, TargetInterface target)
	{
		this.exporter = exporter;
		this.table = table;
		this.target = target;
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		List<Map<String, String>> adList = this.table.exportSelected();
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
