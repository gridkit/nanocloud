package org.gridkit.util.vicontrol.isolate;

import org.gridkit.util.vicontrol.ViNode;
import org.gridkit.util.vicontrol.ViNodeConfig;
import org.gridkit.util.vicontrol.ViNodeProvider;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class IsolateViNodeProvider implements ViNodeProvider {

	@Override
	public boolean verifyNodeConfig(ViNodeConfig config) {
		// TODO
		return true;
	}

	@Override
	public ViNode createNode(String name, ViNodeConfig config) {
		IsolateViNode node = new IsolateViNode(name);
		config.apply(node);
		return node;
	}	
}
