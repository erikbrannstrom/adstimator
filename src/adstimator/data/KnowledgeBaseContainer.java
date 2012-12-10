/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package adstimator.data;

import java.util.Observable;

/**
 *
 * @author erikbrannstrom
 */
public class KnowledgeBaseContainer extends Observable
{
	private KnowledgeBase kb;

	public KnowledgeBaseContainer(KnowledgeBase kb)
	{
		this.kb = kb;
	}

	public KnowledgeBase getKnowledgeBase()
	{
		return kb;
	}

	public void setKnowledgeBase(KnowledgeBase kb)
	{
		this.kb = kb;
		this.notifyObservers();
	}
	
}
