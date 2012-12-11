package adstimator;

import adstimator.gui.GUI;

/**
 * Main application that initializes and displays the GUI to the user.
 *
 * @author erikbrannstrom
 */
public class Main {
	
	public static void main(String[] args)
	{
		if (Setup.isFirstRun()) {
			System.out.println("First time application starts. Run initilization.");
			Setup.init();
		}
		
		GUI frame = new GUI();
		frame.setSize(900, 600);
		frame.setVisible(true);
	}
	
}
