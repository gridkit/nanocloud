package org.gridkit.nanocloud.viengine;

import java.util.List;
import java.util.Map;

public interface ProcessLifecycleListener {

    /**
     * Called then node command is executed.
     * Callback is not used if node is not started via shell.
     */
    public void processStarted(String nodeName, ExecInfo execInfo);

    /**
     * Called then node command has failed to execute.
     * Callback is not used if node is not started via shell.
     */
    public void processExecFailed(String nodeName, ExecFailedInfo execFailedInfo);

    /**
     * Called then node's process has been terminated.
     * Callback is not used if node is not started via shell.
     */
    public void processTerminated(String nodeName, TerminationInfo termInfo);

    public interface ExecInfo {

        public String getNodeName();

        public String getHostname();

        public String getWorkPath();

        public List<String> getCommand();

        public Map<String, String> getAddedEnvironment();
    }

    public interface ExecFailedInfo extends ExecInfo {

        public String getError();
    }

    public interface TerminationInfo {

        public String getNodeName();

        public String getHostname();

        public int getExitCode();
    }
}
