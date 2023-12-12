package org.gridkit.nanocloud.viengine;

import java.util.List;
import java.util.Map;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSessionWrapper;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.telecontrol.AgentEntry;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.StreamCopyService;

abstract class AbstractLaunchAction extends AbstractNodeAction {

    protected InArg<StreamCopyService> streamCopyService = optional(ViConf.SPI_STREAM_COPY_SERVICE);
    protected InArg<RemoteExecutionSession> remotingSession = required(Pragma.RUNTIME_REMOTING_SESSION);

    protected HostControlConsole getControlConsole() {
        return getContext().get(Pragma.RUNTIME_HOST_CONTROL_CONSOLE);
    }

    protected ProcessLifecycleListener getProcessLifecycleListener() {
        List<ProcessLifecycleListener> listeners = getContext().collect(ViConf.JVM_PROCESS_LIFECYCLE_LISTENER + "**", ProcessLifecycleListener.class);
        ProcessLifecycleListener pll = null;
        if (listeners != null && !listeners.isEmpty()) {
            pll = aggregate(listeners);
        }
        return pll;
    }

    protected RemoteExecutionSession getRemotingSession() {
        RemoteExecutionSession session = remotingSession.get();
        List<RemoteExecutionSessionWrapper> wrappers = getContext().collect(Pragma.RUNTIME_REMOTING_SESSION_WRAPER + "**", RemoteExecutionSessionWrapper.class);
        for (RemoteExecutionSessionWrapper ww: wrappers) {
            session = ww.wrap(session);
        }
        return session;

    }

    protected abstract String getSlaveJvmExecCmd();

    protected abstract String getSlaveWorkDir();

    protected abstract Map<String, String> getSlaveEnv();

    protected abstract List<String> getSlaveArgs();

    protected abstract List<String> getSlaveShallowClasspath();

    protected abstract List<ClasspathEntry> getSlaveClasspath();

    protected abstract List<AgentEntry> getAgentEntries();

    protected StreamCopyService getStreamCopyService() {
        return streamCopyService.get();
    }

    protected void setManagedProcess(ManagedProcess process) {

        getContext().set(Pragma.RUNTIME_MANAGED_PROCESS, process);
        getContext().set(Pragma.RUNTIME_EXECUTOR, process.getExecutionService());
        getContext().set(Pragma.RUNTIME_STOP_SWITCH, new StopAction(process));
        getContext().set(Pragma.RUNTIME_KILL_SWITCH, new KillAction(process));
        getContext().set(Pragma.RUNTIME_TEXT_TERMINAL, new ManagedProcessTextTerminal(process));

    }

    protected class LaunchConfig implements ProcessLauncher.LaunchConfig {

        @Override
        public String getNodeName() {
            String nodeName = getContext().get(Pragma.NODE_NAME);
            return nodeName;
        }

        @Override
        public HostControlConsole getControlConsole() {
            return AbstractLaunchAction.this.getControlConsole();
        }

        @Override
        public ProcessLifecycleListener getLifecycleListener() {
            return AbstractLaunchAction.this.getProcessLifecycleListener();
        }

        @Override
        public RemoteExecutionSession getRemotingSession() {
            return AbstractLaunchAction.this.getRemotingSession();
        }

        @Override
        public String getSlaveJvmExecCmd() {
            return AbstractLaunchAction.this.getSlaveJvmExecCmd();
        }

        @Override
        public String getSlaveWorkDir() {
            return AbstractLaunchAction.this.getSlaveWorkDir();
        }

        @Override
        public Map<String, String> getSlaveEnv() {
            return AbstractLaunchAction.this.getSlaveEnv();
        }

        @Override
        public List<String> getSlaveArgs() {
            return AbstractLaunchAction.this.getSlaveArgs();
        }

        @Override
        public List<String> getSlaveShallowClasspath() {
            return AbstractLaunchAction.this.getSlaveShallowClasspath();
        }

        @Override
        public List<ClasspathEntry> getSlaveClasspath() {
            return AbstractLaunchAction.this.getSlaveClasspath();
        }

        @Override
        public List<AgentEntry> getAgentEntries() {
            return AbstractLaunchAction.this.getAgentEntries();
        }

        @Override
        public StreamCopyService getStreamCopyService() {
            return AbstractLaunchAction.this.getStreamCopyService();
        }
    }

    protected static class KillAction extends AbstractStopAction {

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

    protected static class StopAction extends AbstractStopAction {

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

    private ProcessLifecycleListener aggregate(final List<ProcessLifecycleListener> listeners) {
        return new ProcessLifecycleListener() {

            @Override
            public void processStarted(String nodeName, ExecInfo execInfo) {
                for (ProcessLifecycleListener l: listeners) {
                    l.processStarted(nodeName, execInfo);
                }
            }

            @Override
            public void processExecFailed(String nodeName, ExecFailedInfo execFailedInfo) {
                for (ProcessLifecycleListener l: listeners) {
                    l.processExecFailed(nodeName, execFailedInfo);
                }
            }

            @Override
            public void processTerminated(String nodeName, TerminationInfo termInfo) {
                for (ProcessLifecycleListener l: listeners) {
                    l.processTerminated(nodeName, termInfo);
                }
            }
        };
    }
}
