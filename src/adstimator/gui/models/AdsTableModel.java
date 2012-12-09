package adstimator.gui.models;

import adstimator.data.Ads;

public class AdsTableModel extends javax.swing.table.AbstractTableModel
{
	private Ads estimatedAds, existingAds;


	public AdsTableModel(Ads estimatedAds, Ads existingAds)
	{
		this.estimatedAds = estimatedAds;
		this.existingAds = existingAds;
	}

	public void setData(Ads estimatedAds, Ads existingAds)
	{
		this.estimatedAds = estimatedAds;
		this.existingAds = existingAds;
		this.fireTableStructureChanged();
	}

	@Override
	public int getRowCount()
	{
		if (this.estimatedAds == null && this.existingAds == null) {
			return 0;
		} else if (this.existingAds == null) {
			return this.estimatedAds.numInstances();
		} else if (this.estimatedAds == null) {
			return this.existingAds.numInstances();
		}
		return this.estimatedAds.numInstances() + this.existingAds.numInstances();
	}

	@Override
	public int getColumnCount()
	{
		if (this.estimatedAds == null && this.existingAds == null) {
			return 0;
		} else if (this.existingAds == null) {
			return this.estimatedAds.numAttributes();
		}
		return this.existingAds.numAttributes();
	}

	@Override
	public String getColumnName(int col) {
		if (this.estimatedAds == null && this.existingAds == null) {
			return "";
		} else if (this.existingAds == null) {
			return this.estimatedAds.attribute(col).name();
		}
		return this.existingAds.attribute(col).name();
	}

	@Override
	public Object getValueAt(int row, int column)
	{
		if (this.estimatedAds == null && this.existingAds == null) {
			return null;
		} else if (this.estimatedAds == null) {
			return this.getValueHelper(this.existingAds, row, column);
		} else if (this.existingAds == null) {
			return this.getValueHelper(this.estimatedAds, row, column);
		} else {
			// If both estimated and existing ads are set, act is if estimated is listed before existing
			if (row >= this.estimatedAds.numInstances()) {
				return this.getValueHelper(this.existingAds, row-this.estimatedAds.numInstances(), column);
			} else {
				return this.getValueHelper(this.estimatedAds, row, column);
			}
		}
	}

	public boolean isEstimated(int row)
	{
		if (this.estimatedAds == null) {
			return false;
		} else {
			return row < this.estimatedAds.numInstances();
		}
	}

	private Object getValueHelper(Ads ads, int row, int column)
	{
		if (ads.attribute(column).isNumeric()) {
			return String.format("%.6f%%", ads.get(row).value(column)*100);
		} else {
			return ads.get(row).stringValue(column);
		}
	}

}