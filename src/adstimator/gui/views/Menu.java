package adstimator.gui.views;

import adstimator.data.Ads;
import adstimator.data.KnowledgeBase;
import adstimator.data.KnowledgeBaseContainer;
import adstimator.gui.controllers.ExportActionListener;
import adstimator.io.FacebookDataParser;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Extension of the Swing JMenuBar. It contains actions for exporting selected table rows, importing reports from
 * Facebook and creating/deleting/switching knowledge bases. The last two have their logic contained in this class.
 * 
 * The import function uses the FacebookDataParser class to parse a report chosen by the user and hands that data on to
 * the knowledge base.
 * 
 * The knowledge base menu is updated any time the current knowledge base is changed, a new one is created or an 
 * existing one is deleted. It contains some logic, but most of the heavy lifting is delegated to the KnowledgeBase
 * class.
 *
 * @author erikbrannstrom
 */
public class Menu extends JMenuBar
{

	private ExportActionListener exportAction;
	private KnowledgeBaseContainer kbContainer;
	private JMenu menuDatabase;

	/**
	 * Initialize a new menu with an export action instance and the container for the knowledge base.
	 * 
	 * @param exportAction
	 * @param kbContainer 
	 */
	public Menu(ExportActionListener exportAction, KnowledgeBaseContainer kbContainer)
	{
		this.exportAction = exportAction;
		this.kbContainer = kbContainer;
		this.init();
	}

	/**
	 * Initialize and add all menus and actions.
	 */
	private void init()
	{
		JMenu menuFile = new JMenu("File");
		JMenuItem menuItmReport = new JMenuItem("Import Facebook CSV");
		menuItmReport.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(new FileNameExtensionFilter("CSV", "csv"));
				int returnVal = chooser.showOpenDialog(getParent());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					kbContainer.getKnowledgeBase().addAds(new Ads(FacebookDataParser.parse(chooser.getSelectedFile())));
					kbContainer.updated();
				}
			}
		});
		menuItmReport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuFile.add(menuItmReport);

		JMenuItem menuItmExport = new JMenuItem("Export selection");
		menuItmExport.addActionListener(this.exportAction);
		menuItmExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuFile.add(menuItmExport);

		JMenuItem quit = new JMenuItem("Quit");
		quit.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		});
		menuFile.add(quit);

		menuDatabase = new JMenu("Knowledge bases");
		this.updateKBMenu();

		this.add(menuFile);
		this.add(menuDatabase);
	}

	/**
	 * Private method for updating the knowledge base menu.
	 */
	private void updateKBMenu()
	{
		// Empty menu
		this.menuDatabase.removeAll();

		// Add create action
		JMenuItem menuItmNew = new JMenuItem("New KB");
		menuItmNew.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				String name = JOptionPane.showInputDialog(null, "Name the new knowledge base:");
				if (name != null && name.length() > 0) {
					KnowledgeBase kb = new KnowledgeBase(name);
					kb.save();
					kbContainer.setKnowledgeBase(kb);
					updateKBMenu();
				}
			}
		});
		this.menuDatabase.add(menuItmNew);

		// Add delete action
		JMenuItem menuItmDelete = new JMenuItem("Delete current KB");
		menuItmDelete.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{
				int answer = JOptionPane.showConfirmDialog(null,
						String.format("Are you sure you want to delete the knowledge base %s?", kbContainer.getKnowledgeBase().name()),
						"Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
				if (answer == JOptionPane.YES_OPTION) {
					List<KnowledgeBase> kbs = KnowledgeBase.getAll();
					if (kbs.size() > 1) {
						kbContainer.getKnowledgeBase().delete();
						kbs = KnowledgeBase.getAll();
						kbContainer.setKnowledgeBase(kbs.get(0));
						updateKBMenu();
					} else {
						JOptionPane.showMessageDialog(null, "Cannot remove the last KB. Create a new one first.");
					}
				}
			}
		});
		this.menuDatabase.add(menuItmDelete);

		this.menuDatabase.addSeparator();

		// Add all KBs to the menu, mark the current one as selected and register an action for clicks.
		ButtonGroup group = new ButtonGroup();
		for (final KnowledgeBase kb : KnowledgeBase.getAll()) {
			JRadioButtonMenuItem itm = new JRadioButtonMenuItem(kb.name(), kb.id() == this.kbContainer.getKnowledgeBase().id());
			itm.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					kbContainer.setKnowledgeBase(kb);
				}
			});
			group.add(itm);
			this.menuDatabase.add(itm);
		}
	}
}
