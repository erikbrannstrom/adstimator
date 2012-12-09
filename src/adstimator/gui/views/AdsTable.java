/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package adstimator.gui.views;

import adstimator.gui.GUI;
import adstimator.gui.models.AdsTableModel;
import java.awt.Color;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultRowSorter;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author erikbrannstrom
 */
public class AdsTable extends JTable
{
	private AdsTableModel model;
	private DefaultRowSorter<AdsTableModel, Integer> sorter;
	private Map<Integer, List<? extends RowSorter.SortKey>> sortKeyMap;

	public AdsTable(AdsTableModel model)
	{
		super(model);
		this.initContextMenu();
		this.model = model;
		this.sortKeyMap = new HashMap<Integer, List<? extends RowSorter.SortKey>>();
		this.sorter = new TableRowSorter<AdsTableModel>(this.model);
		this.setRowSorter(this.sorter);
	}

	@Override
	public void tableChanged(TableModelEvent tme)
	{
		// If not row sorter is set, we are done.
		if (this.getRowSorter() == null) {
			super.tableChanged(tme);
			return;
		}
		
		int prevNumColumns = this.getColumnCount();
		List<? extends RowSorter.SortKey> sorter = this.getRowSorter().getSortKeys();
		super.tableChanged(tme);
		int currentNumColumns = this.getColumnCount();
		// If number of columns have changed we should switch sorter
		// Else we assume the same sorter should be used
		if (prevNumColumns != currentNumColumns) {
			this.sortKeyMap.put(prevNumColumns, sorter);
			if (this.sortKeyMap.containsKey(currentNumColumns)) {
				sorter = this.sortKeyMap.get(currentNumColumns);
			} else {
				sorter = Arrays.asList(new RowSorter.SortKey(this.model.getColumnCount() - 1, SortOrder.DESCENDING));
			}
			
		}
		this.getRowSorter().setSortKeys(sorter);
	}

	public void setRowFilter(RowFilter<? super AdsTableModel, ? super Integer> rf)
	{
		sorter.setRowFilter(rf);
	}
	
	public List<Map<String, String>> exportSelectedRows(Map<String, String> fixedValues)
	{
		// Get selected rows
		int[] selection = this.getSelectedRows();
		for (int i = 0; i < selection.length; i++) {
			selection[i] = this.convertRowIndexToModel(selection[i]);
		}
		if (selection.length == 0) {
			return null;
		}
		// Create list with a map for each ad
		List<Map<String, String>> adList = new LinkedList<Map<String, String>>();
		for (int row : selection) {
			Map<String, String> adMap = new HashMap<String, String>();
			adMap.put("Body", this.model.getValueAt(row, this.model.findColumn("Body")).toString());
			adMap.put("Image Hash", this.model.getValueAt(row, this.model.findColumn("Image_Hash")).toString());
			adMap.putAll(fixedValues);

			adList.add(adMap);
		}
		return adList;
	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column)
	{
		DefaultTableCellRenderer cr = new DefaultTableCellRenderer();

		// Set color for rows with estimated ads
		if (this.model.isEstimated(this.convertRowIndexToModel(row))) {
			cr.setBackground(new Color(240, 190, 110, 80));
		}

		return cr;
	}

	private void initContextMenu()
	{
		this.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				int r = AdsTable.this.rowAtPoint(e.getPoint());
				if (r >= 0 && r < AdsTable.this.getRowCount()) {
					AdsTable.this.setRowSelectionInterval(r, r);
				} else {
					AdsTable.this.clearSelection();
				}

				int rowindex = AdsTable.this.getSelectedRow();
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
				itm.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent ae)
					{
						int row = AdsTable.this.rowAtPoint(p);
						int column = AdsTable.this.columnAtPoint(p);

						Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
						StringSelection data = new StringSelection(AdsTable.this.getValueAt(row, column).toString());
						c.setContents(data, data);
					}
				});
				menu.add(itm);
				return menu;
			}
		});
	}
}
