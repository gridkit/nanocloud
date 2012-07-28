package org.gridkit.util.vicontrol;

import java.util.Collection;

public interface ViNodeSet {

	/**
	 * Return node by name (or group of nodes for pattern).
	 */
	public ViNode node(String namePattern);

	/**
	 * List non-terminated nodes matching namePattern
	 */	
	public Collection<ViNode> listNodes(String namePattern);
	
	public void shutdown();
	
}
