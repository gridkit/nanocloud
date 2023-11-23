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

	/**
	 * Replaced class with instances that eligible to transparent-rmi.
	 * If result of this call will be passed to remote side, proxy will be created at remote side, and
	 * all method invocations will be passed to original object.
	 */
	public <T> T createRmiProxy(T object);

}
