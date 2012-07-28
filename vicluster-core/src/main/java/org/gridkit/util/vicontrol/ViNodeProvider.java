package org.gridkit.util.vicontrol;

/**
 * This class is used by ViManager to create instances of named ViNodes.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface ViNodeProvider {

	public boolean verifyNodeConfig(ViNodeConfig config);
	
	public ViNode createNode(String name, ViNodeConfig config);
	
}
