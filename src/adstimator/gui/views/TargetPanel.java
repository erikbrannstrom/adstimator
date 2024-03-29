package adstimator.gui.views;

import adstimator.data.KnowledgeBaseContainer;
import adstimator.gui.models.TargetInterface;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * An extension of the Swing JPanel which allows the user to update targeting controls.
 *
 * @author erikbrannstrom
 */
public class TargetPanel extends JPanel implements Observer, TargetInterface
{
	private KnowledgeBaseContainer kbc;
	private JComboBox cmbGender;
	private JComboBox cmbAge;

	/**
	 * Initialize a new targeting panel with a knowledge base container. The panel will automatically add itself as an
	 * observer on the container.
	 * 
	 * @param kbc 
	 */
	public TargetPanel(KnowledgeBaseContainer kbc)
	{
		super(new MigLayout("ins 5"));
		this.kbc = kbc;
		this.init();
		this.kbc.addObserver(this);
	}
	
	@Override
	public Map<String, String> currentTarget()
	{
		Map<String, String> currentTarget = new HashMap<String, String>();
		// Set gender, if other than all is selected
		String val = (String) this.cmbGender.getSelectedItem();
		if (!val.equalsIgnoreCase("All")) {
			currentTarget.put("Gender", val);
		}

		// Set ages, if other than all is selected
		val = (String) this.cmbAge.getSelectedItem();
		if (!val.equalsIgnoreCase("All")) {
			currentTarget.put("Age Min", val.substring(0, val.indexOf("-")));
			currentTarget.put("Age Max", val.substring(val.indexOf("-") + 1));
		}
		return currentTarget;
	}
	
	/**
	 * Private helper method for initializing and adding controls to the panel.
	 */
	private void init()
	{
		this.cmbGender = new JComboBox();
		this.cmbAge = new JComboBox();
		this.add(new JLabel("Target:"));
		this.add(cmbGender);
		this.add(cmbAge);
		this.updateTargetControls();
	}
	
	/**
	 * Private method which should be called when a new report has been imported in order to repopulate the targeting
	 * options.
	 */
	private void updateTargetControls()
	{
		Map<String, List<String>> targets = this.kbc.getKnowledgeBase().targets();
		this.cmbGender.removeAllItems();
		for (String gender : targets.get("Gender")) {
			this.cmbGender.addItem(gender);
		}
		
		this.cmbAge.removeAllItems();
		for (String age : targets.get("Age")) {
			this.cmbAge.addItem(age);
		}
	}

	/**
	 * Updates the targeting controls so that they match the ones in the current knowledge base. Called automatically
	 * when the knowledge base in the container is changed.
	 * 
	 * @param o
	 * @param o1 
	 */
	@Override
	public void update(Observable o, Object o1)
	{
		this.updateTargetControls();
	}
	
}
