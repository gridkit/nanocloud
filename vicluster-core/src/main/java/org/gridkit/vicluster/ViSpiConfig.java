package org.gridkit.vicluster;

import java.util.List;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSessionWrapper;
import org.gridkit.vicluster.telecontrol.AgentEntry;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.StreamCopyService;

public interface ViSpiConfig {

	public String getNodeName();

	public String getNodeType();

	public java.util.Map<String, Object> getConfigMap();
	
	public ViNode getNodeInstance();

	public NodeFactory getNodeFactory();

	public ManagedProcess getManagedProcess();

	public List<String> getSlaveArgs();

	public java.util.Map<String, String> getSlaveEnv();

	public String getSlaveWorkDir();

	public List<ClasspathEntry> getSlaveClasspath();

	public List<AgentEntry> getSlaveAgents();

	public String getJvmExecCmd();

	public RemoteExecutionSession getRemotingSession();

	public RemoteExecutionSessionWrapper getInstrumentationWrapper();

	public boolean isInstrumentationWrapperApplied();
	
	public ProcessLauncher getProcessLauncher();

	public HostControlConsole getControlConsole();

	public CloudContext getCloudContext();

	public StreamCopyService getStreamCopyService();
	
	public <T> T get(String key);

    boolean isConfigTraceEnbaled();

    boolean shouldDumpConfigOnFailure();
}
