package org.gridkit.nanocloud.viengine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.telecontrol.AgentEntry;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;


public class LaunchProcessAction extends AbstractLaunchAction {

    InArg<ProcessLauncher> launcher = required(Pragma.RUNTIME_PROCESS_LAUNCHER);
    InArg<HostControlConsole> controlConsole = required(Pragma.RUNTIME_HOST_CONTROL_CONSOLE);
    InArg<List<ClasspathEntry>> classpath = required(Pragma.RUNTIME_CLASSPATH);
    InArg<List<String>> shallowClasspath = optional(Pragma.RUNTIME_SHALLOW_CLASSPATH);
    InArg<List<AgentEntry>> agents = required(Pragma.RUNTIME_AGENTS);
    InArg<String> jvmExec = required(ViConf.JVM_EXEC_CMD);
    InArg<String> jvmWorkDir = optional(ViConf.JVM_WORK_DIR);



    @Override
    protected void run() {
        ManagedProcess process = launcher.get().launchProcess(new LaunchConfig());
        setManagedProcess(process);
    }

    @Override
    protected String getSlaveJvmExecCmd() {
        return jvmExec.get();
    }

    @Override
    protected String getSlaveWorkDir() {
        return jvmWorkDir.get();
    }

    @Override
    protected Map<String, String> getSlaveEnv() {
        return collectEnvironment();
    }

    @Override
    protected List<String> getSlaveArgs() {
        return collectJvmOptions();
    }

    @Override
    protected List<String> getSlaveShallowClasspath() {
        return shallowClasspath.get();
    }

    @Override
    protected List<ClasspathEntry> getSlaveClasspath() {
        return classpath.get();
    }

    @Override
    protected List<AgentEntry> getAgentEntries() {
        return agents.get();
    }

    private Map<String, String> collectEnvironment() {
        List<String> keys = getContext().match(ViConf.JVM_ENV_VAR + "**");
        if (!keys.isEmpty()) {
            Map<String, String> env = new LinkedHashMap<String, String>();
            for(String key: keys) {
                String vn = key.substring(ViConf.JVM_ENV_VAR.length());
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
        List<String> keys = getContext().match(ViConf.JVM_ARGUMENT + "**");
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
}
