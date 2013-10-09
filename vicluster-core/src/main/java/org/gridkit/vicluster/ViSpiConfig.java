package org.gridkit.vicluster;

import java.util.List;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

public interface ViSpiConfig {

	public String getNodeName();

	public String getNodeType();

	public java.util.Map<String, Object> getConfigMap();
	
	public ViNode getNodeInstance();

	public NodeFactory getNodeFactory();

	public ManagedProcess getManagedProcess();

	public List<String> getJvmArgs();

	public List<ClasspathEntry> getJvmClasspath();

	public String getJvmExecCmd();

	public RemoteExecutionSession getRemotingSession();

	public ProcessLauncher getProcessLauncher();

	public HostControlConsole getControlConsole();

	public CloudContext getCloudContext();
	
	public <T> T get(String key);
}
