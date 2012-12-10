/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package adstimator.gui.controllers;

import adstimator.gui.GUI;
import adstimator.gui.views.AdsTable;
import adstimator.gui.views.TargetPanel;
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
	private final TargetPanel targetPanel;

	public ExportActionListener(Exporter exporter, AdsTable table, TargetPanel targetPanel)
	{
		this.exporter = exporter;
		this.table = table;
		this.targetPanel = targetPanel;
	}

	@Override
	public void actionPerformed(ActionEvent ae)
	{
		List<Map<String, String>> adList = this.table.exportSelectedRows();
		Map<String, String> additions = this.targetPanel.currentTarget();
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
