package org.gridkit.nanocloud.telecontrol;

import java.util.Map;

import org.gridkit.vicluster.telecontrol.ManagedProcess;

public interface ProcessLauncher {

	public abstract ManagedProcess createProcess(Map<String, Object> configuration);

}
