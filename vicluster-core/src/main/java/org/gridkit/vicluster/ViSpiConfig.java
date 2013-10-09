package org.gridkit.vicluster;

import java.util.List;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

public interface ViSpiConfig {

	public abstract ViNode getNodeInstance();

	public abstract NodeFactory getNodeFactory();

	public abstract ManagedProcess getManagedProcess();

	public abstract List<String> getJvmArgs();

	public abstract List<ClasspathEntry> getJvmClasspath();

	public abstract String getJvmExecCmd();

	public abstract RemoteExecutionSession getRemotingSession();

	public abstract ProcessLauncher getProcessLauncher();

	public abstract HostControlConsole getControlConsole();

	public abstract CloudContext getCloudContext();

}
