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
		this.setKnowledgeBase(kb);
	}
	
	public KnowledgeBaseContainer(int id)
	{
		this.setKnowledgeBase(id);
	}

	public KnowledgeBase getKnowledgeBase()
	{
		return kb;
	}

	public final void setKnowledgeBase(KnowledgeBase kb)
	{
		if (this.kb != null && kb.id() == this.kb.id()) {
			return;
		}
		this.kb = kb;
		this.notifyObservers();
	}
	
	public final void setKnowledgeBase(int id)
	{
		this.setKnowledgeBase(KnowledgeBase.find(id));
	}
	
}
