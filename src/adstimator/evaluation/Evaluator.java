package adstimator.evaluation;

/**
 * Abstract class that define the minimum requirements for evaluators.
 * 
 * Note that this could technically have been an interface, however to more easily be able to add common features in
 * future development it was left as an abstract class.
 * 
 * @author erikbrannstrom
 */
public abstract class Evaluator
{
	/**
	 * A textual description of the results from the evaluator.
	 * 
	 * @return Results as a string
	 */
	public abstract String result();
	
	/**
	 * Description of what the class aims to evaluate.
	 * 
	 * @return Description of evaluation
	 */
	public abstract String description();
}