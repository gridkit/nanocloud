package org.gridkit.nanocloud.telecontrol;

import java.util.Map;

import org.gridkit.vicluster.ViNode;

public interface NodeFactory {

	public ViNode createViNode(Map<String, Object> config);
	
}
