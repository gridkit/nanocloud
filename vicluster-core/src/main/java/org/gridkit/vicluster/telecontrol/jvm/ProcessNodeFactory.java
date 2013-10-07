package org.gridkit.vicluster.telecontrol.jvm;

import java.io.IOException;
import java.util.Map;

import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.vicluster.ViEngine.SpiContext;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViNode;

public class ProcessNodeFactory implements NodeFactory {

	@Override
	public ViNode createViNode(Map<String, Object> config) {
		try {
			SpiContext conf = ViEngine.asSpiConfig(config);
			String name = (String) config.get(ViConf.NODE_NAME);
			ManagedProcess mp = conf.getManagedProcess();
			ProcessNode node = new ProcessNode(name, config, mp);
			return node;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
