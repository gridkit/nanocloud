package org.gridkit.nanocloud.telecontrol;

import java.util.List;
import java.util.Map;

import org.gridkit.nanocloud.viengine.ProcessLifecycleListener;
import org.gridkit.vicluster.telecontrol.AgentEntry;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.StreamCopyService;

public interface ProcessLauncher {

    public interface LaunchConfig {

        public String getNodeName();

        public HostControlConsole getControlConsole();

        public RemoteExecutionSession getRemotingSession();

        public ProcessLifecycleListener getLifecycleListener();

        public String getSlaveJvmExecCmd();

        public String getSlaveWorkDir();

        public Map<String, String> getSlaveEnv();

        public List<String> getSlaveArgs();

        public List<String> getSlaveShallowClasspath();

        public List<ClasspathEntry> getSlaveClasspath();

        public List<AgentEntry> getAgentEntries();

        public StreamCopyService getStreamCopyService();

    }

    public ManagedProcess launchProcess(LaunchConfig config);

}
