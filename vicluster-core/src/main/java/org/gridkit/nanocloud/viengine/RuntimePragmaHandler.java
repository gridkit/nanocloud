package org.gridkit.nanocloud.viengine;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.WritableSpiConfig;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

class RuntimePragmaHandler implements PragmaHandler {

    @Override
    public void configure(PragmaWriter context) {
    }

    @Override
    public void init(PragmaWriter context) {
    }

    @Override
    public Object query(PragmaWriter context, String key) {
        if (key.startsWith(ViConf.JVM_PROCESS_LIFECYCLE_LISTENER)) {
            return context.get(key);
        }
        if (key.equals(ViConf.RUNTIME_EXIT_CODE)) {
            return exitCode(context.<ManagedProcess>get(Pragma.RUNTIME_MANAGED_PROCESS));
        }
        else if (key.equals(ViConf.RUNTIME_EXIT_CODE_FUTURE)) {
            return exitCodeFuture(context.<ManagedProcess>get(Pragma.RUNTIME_MANAGED_PROCESS));
        }
        else if (key.equals(ViConf.RUNTIME_HOST)) {
               return context.<HostControlConsole>get(Pragma.RUNTIME_HOST_CONTROL_CONSOLE).getHostname();
        }
        else if (key.equals(ViConf.RUNTIME_PID)) {
            throw new UnsupportedOperationException("TODO implement slave PID retrival");
        }
        else if (key.equals(ViConf.RUNTIME_EXECUTION_SUSPENDED)) {
            String suspended = context.get(key);
            return suspended == null ? "false" : suspended;
        }
        else {
            throw new IllegalArgumentException("Unknown pragma '" + key + "'");
        }
    }

    @Override
    public void setup(PragmaWriter context, Map<String, Object> config) {
        // allow all
    }

    @Override
    public void apply(PragmaWriter context, Map<String, Object> values) {
        // TODO suspension support
        throw new IllegalArgumentException("Cannot write " + values.keySet().toString() + " pragma");
    }

    private Object exitCode(ManagedProcess managedProcess) {
        try {
            FutureEx<Integer> exitCode = managedProcess.getExitCodeFuture();
            if (exitCode.isDone()) {
                return String.valueOf(exitCode.get());
            }
            else {
                return null;
            }
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private Object exitCodeFuture(ManagedProcess managedProcess) {
        return managedProcess.getExitCodeFuture();
    }

    @SuppressWarnings("unused")
    private void suspendOrResume(ViEngine engine, WritableSpiConfig writableConfig, boolean value) {
        throw new UnsupportedOperationException("Suspend / resume is not supported");
    }
}
