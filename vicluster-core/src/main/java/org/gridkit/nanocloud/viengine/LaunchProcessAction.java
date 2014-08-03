package org.gridkit.nanocloud.viengine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    InArg<String> jvmWorkDir = optional(JvmConf.JVM_WORK_DIR);
    
    @Override
    protected void run() {
        Map<String, String> envVars = collectEnvironment();
        List<String> jvmOptions = collectJvmOptions();
        
        ManagedProcess process = launcher.get().launchProcess(new LaunchConfig(envVars, jvmOptions));
        getContext().set(Pragma.RUNTIME_EXECUTOR, process.getExecutionService());
        getContext().set(Pragma.RUNTIME_STOP_SWITCH, new StopAction(process));
        getContext().set(Pragma.RUNTIME_KILL_SWITCH, new KillAction(process));
        getContext().set(Pragma.RUNTIME_TEXT_TERMINAL, new ManagedProcessTextTerminal(process));
    }
    
    private Map<String, String> collectEnvironment() {
        List<String> keys = getContext().match(JvmConf.JVM_ENV_VAR + "**");
        if (!keys.isEmpty()) {
            Map<String, String> env = new LinkedHashMap<String, String>();
            for(String key: keys) {
                String vn = key.substring(JvmConf.JVM_ENV_VAR.length());
                String vv = getContext().get(key);
                if (vv.length() == 1 && vv.charAt(0) == '\00') {
                    vv = null;
                }
                env.put(vn, vv);                
            }
            return env;
        }
        else {
            return null;
        }
    }

    private List<String> collectJvmOptions() {
        List<String> keys = getContext().match(JvmConf.JVM_ARGUMENT + "**");
        List<String> options = new ArrayList<String>();
        for(String key: keys) {
            String o = getContext().get(key);
            if (o.startsWith("|")) {
                String[] opts = o.split("\\|");
                for(String oo: opts) {
                    addOption(options, oo);
                }
            }
            else {
                addOption(options, o);
            }
        }
        return options;
    }

    private void addOption(List<String> options, String o) {
        o = o.trim();
        if (o.length() > 0) {
            options.add(o);
        }
    }

    private class LaunchConfig implements ProcessLauncher.LaunchConfig {

        private Map<String, String> environment;
        private List<String> jvmOptions;
        
        public LaunchConfig(Map<String, String> environment, List<String> jvmOptions) {
            this.environment = environment;
            this.jvmOptions = jvmOptions;
        }

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
            return jvmWorkDir.get();
        }

        @Override
        public Map<String, String> getSlaveEnv() {
            return environment;
        }

        @Override
        public List<String> getSlaveArgs() {
            return jvmOptions;
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
