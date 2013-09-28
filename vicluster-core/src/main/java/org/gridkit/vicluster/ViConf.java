package org.gridkit.vicluster;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.gridkit.util.concurrent.FutureEx;

public class ViConf extends GenericConfig {

	public static final String NODE_NAME = "node:name";
	public static final String NODE_TYPE = "node:type";
	public static final String RUNTIME_PID = "runtime:pid";
	public static final String RUNTIME_HOST = "runtime:host";
	public static final String RUNTIME_EXIT_CODE = "runtime:exitCode";
	public static final String RUNTIME_EXIT_CODE_FUTURE = "runtime:exitCode.future";
	public static final String CONSOLE_STD_IN = "console:stdIn";
	public static final String CONSOLE_STD_OUT_ECHO = "console:stdOut.echo";
	public static final String CONSOLE_STD_OUT = "console:stdOut";
	public static final String CONSOLE_STD_ERR_ECHO = "console:stdErr.echo";
	public static final String CONSOLE_STD_ERR = "console:stdErr";
	public static final String CONSOLE_SILENT_SHUTDOWN = "console:silent-shutdown";
	
	public static final String HOOK = "hook:";
	
	public static final String ACTIVATED_REMOTE_HOOK = "#remote-hook:";
	public static final String ACTIVATED_HOST_HOOK = "#host-hook:";

	
	public static boolean isVanilaProp(String key) {
		return key.indexOf(':') < 0 && key.indexOf('#') < 0;
	}

	public static boolean isHook(String key) {
		return key.startsWith(HOOK);
	}

	public static boolean isRuntime(String key) {
		return key.startsWith("runtime:");
	}

	public static boolean isNode(String key) {
		return key.startsWith("node:");
	}

	public static boolean isConsole(String key) {
		return key.startsWith("console:");
	}

	public static boolean isTemporary(String key) {
		return key.startsWith("#");
	}
	
	protected Map<String, Object> config;
	
	public ViConf(Map<String, Object> config) {
		this.config = config;
	}

	@Override
	Object readRawProp(String name) {
		return config.get(name);
	}
	
	@Override
	void setRawProp(String name, Object value) {
		config.put(name, value);		
	}

	@PropName(NODE_NAME)
	public String getNodeName() {
		return readString();
	}

	@PropName(NODE_TYPE)
	public String getNodeType() {
		return readString();
	}	
	
	@PropName(RUNTIME_PID)
	public String getRuntimePid() {
		return readString();
	}

	@PropName(RUNTIME_HOST)
	public String getRuntimeHost() {
		return readString();
	}

	@PropName(RUNTIME_EXIT_CODE)
	public String getExitCode() {
		return readString();
	}

	@PropName(RUNTIME_EXIT_CODE_FUTURE)
	public FutureEx<Integer> getExitCodeFuture() {
		return readObject();
	}

	@PropName(CONSOLE_STD_IN)
	public InputStream getConsoleStdIn() {
		return readObject();
	}

	@PropName(CONSOLE_STD_OUT_ECHO)
	@Default("true")
	public boolean getConsoleStdOutEcho() {
		return readBoolean();
	}

	@PropName(CONSOLE_STD_OUT)
	public OutputStream getConsoleStdOut() {
		return readObject();
	}

	@PropName(CONSOLE_STD_ERR_ECHO)
	@Default("true")
	public boolean getConsoleStdErrEcho() {
		return readBoolean();
	}
	
	@PropName(CONSOLE_STD_ERR)
	public OutputStream getConsoleStdErr() {
		return readObject();
	}
	
	@PropName(CONSOLE_SILENT_SHUTDOWN)
	@Default("true")
	public boolean getSilenceOutputOnShutdown() {
		return readBoolean();
	}	
}
