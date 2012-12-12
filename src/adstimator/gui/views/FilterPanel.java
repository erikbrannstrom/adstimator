package adstimator.gui.views;

import adstimator.gui.models.AdsTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.miginfocom.swing.MigLayout;

/**
 * Extension of the Swing JPanel which is used to create filters for an AdsTable.
 * 
 * The panel contains a combobox for selecting the type of ads to be shown (All, Existing or Suggestions) and a text
 * field for free text, case-insensitive filtering.
 *
 * @author erikbrannstrom
 */
public class FilterPanel extends JPanel
{
	private AdsTable table;
	private JComboBox cmbFilterType;
	private JTextField filterText;

	/**
	 * Create a new filter panel and connect it to a table which will be automatically updated when these controls
	 * change.
	 * 
	 * @param table Table to be filtered
	 */
	public FilterPanel(AdsTable table)
	{
		super(new MigLayout("fillx, ins 5"));
		this.table = table;
		this.init();
	}

	/**
	 * Add all components and register listeners.
	 */
	private void init()
	{
		this.add(new JLabel("Filters:"), "gap 0 5, align left");
		this.cmbFilterType = new JComboBox(Arrays.asList("All", "Existing", "Suggestions").toArray(new String[0]));
		cmbFilterType.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				updateFilter();
			}
		});

		this.add(this.cmbFilterType, "gap 0 5, align left");
		this.filterText = new JTextField();
		// Whenever filterText changes, update the filter on the table.
		this.filterText.getDocument().addDocumentListener(
				new DocumentListener()
				{
					@Override
					public void changedUpdate(DocumentEvent e)
					{
						updateFilter();
					}

					@Override
					public void insertUpdate(DocumentEvent e)
					{
						updateFilter();
					}

					@Override
					public void removeUpdate(DocumentEvent e)
					{
						updateFilter();
					}
				});
		this.add(this.filterText, "align left, pushx 100, growx 100, gap 0 0");
	}

	/**
	 * Private method which should be called whenever any of the filter options are updated.
	 */
	private void updateFilter()
	{
		// Set type filter
		RowFilter<AdsTableModel, Integer> typeFilter = new RowFilter<AdsTableModel, Integer>()
		{
			@Override
			public boolean include(RowFilter.Entry<? extends AdsTableModel, ? extends Integer> entry)
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
						if (cellValue.toLowerCase().indexOf(filterText.getText().toLowerCase()) > -1) {
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
		this.table.setRowFilter(typeFilter);
	}
}
