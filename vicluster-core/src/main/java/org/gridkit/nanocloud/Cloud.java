package org.gridkit.nanocloud;

import java.util.Collection;

import org.gridkit.vicluster.ViNode;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface Cloud {

	/**
	 * Return node by name (or group of nodes for pattern).
	 */
	public ViNode node(String nameOrSelector);

	public ViNode nodes(String... nameOrSelector);

	/**
	 * List non-terminated nodes matching namePattern
	 */	
	public Collection<ViNode> listNodes(String nameOrSelector);
	
	public void shutdown();

}
