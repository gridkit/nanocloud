package org.gridkit.util.vicontrol;

import java.util.Map;


/**
 * {@link ViHost} represents a service providing {@link ViNode}. It cloud be in-JVM nodes or real JVMs.
 * 
 * Some {@link ViHost}s support on-demand node configuration: you obtain node first then configure it then start. But other may require for configuration up front, so it is generally advisable to use {@link #allocate(String, ViNodeConfig)} method to obtain node object.
 * 
 * @author Alexey Ragozin
 */
public interface ViHost {
	
	public ViNode allocate(String nodeName, ViNodeConfig config);

	public ViNode get(String nodeName);
	
	public Map<String, ViNode> listNodes();
	
	public void shutdown();
}
