package org.gridkit.nanocloud.telecontrol;

import java.util.concurrent.ExecutionException;

import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.PragmaHandler;
import org.gridkit.vicluster.ViEngine.WritableSpiConfig;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

public class RuntimePragmaSupport implements PragmaHandler {

    @Override
    public Object get(String key, ViEngine engine) {
        if (key.equals(ViConf.RUNTIME_EXIT_CODE)) {
            return exitCode(engine.getConfig().getManagedProcess());
        }
        else if (key.equals(ViConf.RUNTIME_EXIT_CODE_FUTURE)) {
            return exitCodeFuture(engine.getConfig().getManagedProcess());
        }
        else if (key.equals(ViConf.RUNTIME_HOST)) {
               return engine.getConfig().getControlConsole().getHostname();
        }
        else if (key.equals(ViConf.RUNTIME_PID)) {
            throw new UnsupportedOperationException("TODO implement slave PID retrival");
        }
        else if (key.equals(ViConf.RUNTIME_EXECUTION_SUSPENDED)) {
            String suspended = engine.getConfig().get(key);
            return suspended == null ? "false" : suspended;
        }
        else {
            throw new IllegalArgumentException("Unknown pragma '" + key + "'");
        }
    }

    @Override
    public void set(String key, Object value, ViEngine engine, WritableSpiConfig writableConfig) {
        if (key.equals(ViConf.RUNTIME_EXECUTION_SUSPENDED)) {
            suspendOrResume(engine, writableConfig, Boolean.valueOf((String)value));
        }
        else {
            throw new IllegalArgumentException("Pragma '" + key + "' is not known or not writable");
        }
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

    private void suspendOrResume(ViEngine engine, WritableSpiConfig writableConfig, boolean value) {
        throw new UnsupportedOperationException("Suspend / resume is not supported");
    }
}
