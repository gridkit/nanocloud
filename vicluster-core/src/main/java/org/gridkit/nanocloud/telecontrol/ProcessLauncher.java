package org.gridkit.nanocloud.telecontrol;

import java.util.List;
import java.util.Map;

import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

public interface ProcessLauncher {

    public interface LaunchConfig {

        public String getNodeName();

        public HostControlConsole getControlConsole();

        public RemoteExecutionSession getRemotingSession();
        
        public String getSlaveJvmExecCmd();

        public String getSlaveWorkDir();

        public Map<String, String> getSlaveEnv();

        public List<String> getSlaveArgs();

        public List<ClasspathEntry> getSlaveClasspath();
        
    }

    public ManagedProcess launchProcess(LaunchConfig config);
    
    @Deprecated
	public ManagedProcess createProcess(Map<String, Object> configuration);

}
