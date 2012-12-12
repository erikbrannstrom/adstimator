package adstimator.gui.models;

import adstimator.data.Ads;

/**
 * Model used by the AdsTable.
 * 
 * The AdsTableModel can contain two separate sets of data; one with ads whose click rates are estimated and one with 
 * real click rates. Most logic deals with having these two data sets act as one as seen from the outside of this class.
 * 
 * @author erikbrannstrom
 */
public class AdsTableModel extends javax.swing.table.AbstractTableModel
{
	private Ads estimatedAds, existingAds;

	/**
	 * Create a new table model with the estimated and existing ads. Either, or both, can be null.
	 * 
	 * @param estimatedAds Estimated ads, can be null
	 * @param existingAds Existing ads, can be null
	 */
	public AdsTableModel(Ads estimatedAds, Ads existingAds)
	{
		this.estimatedAds = estimatedAds;
		this.existingAds = existingAds;
	}

	/**
	 * Change the data contained in this object. After the new data sets have been applied, the method will call any
	 * table using this model to inform of a potential change in structure.
	 * 
	 * @param estimatedAds Estimated ads, can be null
	 * @param existingAds  Existing ads, can be null
	 */
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

	/**
	 * Returns whether or not a row belongs to the set of estimated ads.
	 * 
	 * @param row
	 * @return true if the row is an estimated ad
	 */
	public boolean isEstimated(int row)
	{
		if (this.estimatedAds == null) {
			return false;
		} else {
			return row < this.estimatedAds.numInstances();
		}
	}

	/**
	 * Private helper for returning a string value for the actual cell values.
	 * 
	 * @param ads
	 * @param row
	 * @param column
	 * @return 
	 */
	private Object getValueHelper(Ads ads, int row, int column)
	{
		if (ads.attribute(column).isNumeric()) {
			return String.format("%.6f%%", ads.get(row).value(column)*100);
		} else {
			return ads.get(row).stringValue(column);
		}
	}

}