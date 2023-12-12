package org.gridkit.nanocloud.viengine;

import java.util.List;
import java.util.Map;

import org.gridkit.nanocloud.telecontrol.isolate.IsolateConfig;
import org.gridkit.nanocloud.telecontrol.isolate.IsolateRemoteSessionWrapper;
import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.isolate.IsolateProps;
import org.gridkit.vicluster.telecontrol.AgentEntry;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;


public class LaunchIsolateAction extends AbstractLaunchAction {

    InArg<String> name = required(IsolateProps.NAME);
    InArg<String> isNoMarshal = optional(IsolateConfig.NO_MARSHAL);
    InArg<String> isShareAllClasses = optional(IsolateConfig.SHARE_ALL_CLASSES);
    InArg<List<ClasspathEntry>> classpath = required(Pragma.RUNTIME_CLASSPATH);
    InArg<List<String>> shallowClasspath = optional(Pragma.RUNTIME_SHALLOW_CLASSPATH);

    @Override
    protected void run() {

        ManagedProcess process;

        // forbid double isolation
        getContext().set(Pragma.RUNTIME_REMOTING_SESSION_WRAPER + IsolateRemoteSessionWrapper.class.getName(), null);

        if ("true".equals(isNoMarshal.get())) {
            Isolate isolate = new Isolate(name.get());
            if ("true".equals(isShareAllClasses.get())) {
                isolate.addPackage("", false);
            }
            isolate.start();
            // TODO isolate configuration
            process = new ManagedNoMarshalIsolate(isolate);
        } else {
            IsolateLauncher launcher = new IsolateLauncher();
            process = launcher.launchProcess(new LaunchConfig());
        }

        setManagedProcess(process);
    }

    @Override
    protected String getSlaveJvmExecCmd() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getSlaveWorkDir() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Map<String, String> getSlaveEnv() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<String> getSlaveArgs() {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }
}
