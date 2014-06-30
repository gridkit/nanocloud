package org.gridkit.nanocloud.viengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;


public class LaunchProcessAction extends AbstractNodeAction {

    InArg<ProcessLauncher> launcher = required(Pragma.RUNTIME_PROCESS_LAUNCHER);
    InArg<RemoteExecutionSession> remotingSession = required(Pragma.RUNTIME_REMOTING_SESSION);
    InArg<HostControlConsole> controlConsole = required(Pragma.RUNTIME_HOST_CONTROL_CONSOLE);
    InArg<List<ClasspathEntry>> classpath = required(Pragma.RUNTIME_CLASSPATH);
    InArg<String> jvmExec = required(JvmConf.JVM_EXEC_CMD);
    
    @Override
    protected void run() {
        ManagedProcess process = launcher.get().launchProcess(new LaunchConfig());
        getContext().set(Pragma.RUNTIME_EXECUTOR, process.getExecutionService());
        getContext().set(Pragma.RUNTIME_STOP_SWITCH, new StopAction(process));
        getContext().set(Pragma.RUNTIME_KILL_SWITCH, new KillAction(process));
        getContext().set(Pragma.RUNTIME_TEXT_TERMINAL, new ManagedProcessTextTerminal(process));
    }
    
    private class LaunchConfig implements ProcessLauncher.LaunchConfig {

        @Override
        public String getNodeName() {
            String nodeName = getContext().get(Pragma.NODE_NAME);
            return nodeName;
        }

        @Override
        public HostControlConsole getControlConsole() {
            return controlConsole.get();
        }

        @Override
        public RemoteExecutionSession getRemotingSession() {
            return remotingSession.get();
        }

        @Override
        public String getSlaveJvmExecCmd() {
            return jvmExec.get();
        }

        @Override
        public String getSlaveWorkDir() {
            // TODO 
            return null;
        }

        @Override
        public Map<String, String> getSlaveEnv() {
            // TODO 
            return null;
        }

        @Override
        public List<String> getSlaveArgs() {
            // TODO 
            return new ArrayList<String>();
        }

        @Override
        public List<ClasspathEntry> getSlaveClasspath() {
            return classpath.get();
        }
    }
    
    private static class KillAction extends AbstractStopAction {
        
        private ManagedProcess process;

        public KillAction(ManagedProcess process) {
            super(false, true);
            this.process = process;
        }

        @Override
        protected void stop() {
            process.destroy();
        }
    }

    private static class StopAction extends AbstractStopAction {
        
        private ManagedProcess process;

        public StopAction(ManagedProcess process) {
            super(true, true);
            this.process = process;
        }

        @Override
        protected void stop() {            
            process.destroy();
        }
    }    
}
