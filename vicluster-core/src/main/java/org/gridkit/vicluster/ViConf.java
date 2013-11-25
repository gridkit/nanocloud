package org.gridkit.vicluster;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.ViConfigurable.Delegate;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.gridkit.zeroio.WriterOutputStream;

public class ViConf extends GenericConfig implements ViSpiConfig {

	
	public static final String NODE_NAME = "node:name";
	public static final String NODE_TYPE = "node:type";
	
	public static final String NODE_TYPE__LOCAL = ViProps.NODE_TYPE_LOCAL;
	public static final String NODE_TYPE__ISOLATE = ViProps.NODE_TYPE_ISOLATE;
	public static final String NODE_TYPE__REMOTE = ViProps.NODE_TYPE_REMOTE;
	
	public static final String RUNTIME_PID = "runtime:pid";
	public static final String RUNTIME_HOST = "runtime:host";
	public static final String RUNTIME_EXIT_CODE = "runtime:exitCode";
	public static final String RUNTIME_EXIT_CODE_FUTURE = "runtime:exitCode.future";
	public static final String CONSOLE_STD_IN = "console:stdIn";
	public static final String CONSOLE_STD_OUT_ECHO = "console:stdOut.echo";
	/** Overrides stream for echo, introduced to support Isolate nodes */
	public static final String CONSOLE_STD_OUT_ECHO_STREAM = "console:stdOut.echo.stream";
	public static final String CONSOLE_STD_OUT = "console:stdOut";
	public static final String CONSOLE_STD_ERR_ECHO = "console:stdErr.echo";
	/** Overrides stream for echo, introduced to support Isolate nodes */
	public static final String CONSOLE_STD_ERR_ECHO_STREAM = "console:stdErr.echo.stream";
	public static final String CONSOLE_STD_ERR = "console:stdErr";
	public static final String CONSOLE_ECHO_PREFIX = "console:echo-prefix";
	public static final String CONSOLE_FLUSH = "console:flush";
	public static final String CONSOLE_SILENT_SHUTDOWN = "console:silent-shutdown";

	public static final String JVM_EXEC_CMD = "jvm:exec-command";
	public static final String JVM_ARGUMENT = JvmProps.JVM_XX;
	public static final String JVM_WORK_DIR = JvmProps.JVM_WORK_DIR;
	public static final String JVM_ENV_VAR = JvmProps.JVM_ENV;

	public static final String CLASSPATH_TWEAK = "classpath:tweak:";

	public static final String REMOTE_HOST = "remote:host";
	public static final String REMOTE_ACCOUNT = "remote:account";
	public static final String REMOTE_HOST_CONFIG = "remote:host-config";

	public static final String LOG = "log:";
	public static final String LOG_LOGGER = "log:logger:";
	public static final String LOG_LOGGER_PROVIDER = "log:logger:provider";
	public static final String LOG_LOGGER_ROOT = "log:logger:root";
	
	public static final String HOOK = "hook:";

	public static final String HOOK_CLASSPATH_BUILDER = "hook:classpath-builder";
	public static final String HOOK_JVM_ARGUMENTS_BUIDLER = "hook:jvm-arguments-builder";
	public static final String HOOK_JVM_ENV_VARS_BUIDLER = "hook:jvm-env-vars-builder";
	public static final String HOOK_NODE_INITIALIZER = "hook:node-initializer";

	public static final String PRAGMA_HANDLER = "pragma-handler:";
	public static final String PRAGMA_HANDLER__CONSOLE = "pragma-handler:console";
	public static final String PRAGMA_HANDLER__LOG = "pragma-handler:log";
	public static final String PRAGMA_HANDLER__HOOK = "pragma-handler:hook";
	public static final String PRAGMA_HANDLER__CLASSPATH = "pragma-handler:classpath";
	public static final String PRAGMA_HANDLER__JVM = "pragma-handler:jvm";
	public static final String PRAGMA_HANDLER__NODE = "pragma-handler:node";
	public static final String PRAGMA_HANDLER__RUNTIME = "pragma-handler:runtime";
	public static final String PRAGMA_HANDLER__REMOTE = "pragma-handler:remote";
	
	public static final String TYPE_HANDLER = "type-handler:";

	public static final String SPI_CLOUD_CONTEXT = "#spi:cloud-context";
	public static final String SPI_KILL_SWITCH = "#spi:kill-switch";
	public static final String SPI_EPITAPH = "#spi:epitaph";
	public static final String SPI_REMOTING_SESSION = "#spi:remoting-session";
	public static final String SPI_JVM_EXEC_CMD = JVM_EXEC_CMD; // TODO "#spi:jvm-exec-cmd";
	public static final String SPI_JVM_ARGS = "#spi:jvm-args";
	public static final String SPI_JVM_CLASSPATH = "#spi:jvm-classpath";
	public static final String SPI_CONTROL_CONSOLE = "#spi:control-console";
	public static final String SPI_PROCESS_LAUNCHER = "#spi:process-launcher";
	public static final String SPI_MANAGED_PROCESS = "#spi:managed-process";
	public static final String SPI_NODE_FACTORY = "#spi:node-factory";
	public static final String SPI_NODE_INSTANCE = "#spi:node-instance";
	public static final String SPI_LOGGER = "#spi:logger";
	
	public static final String ACTIVATED_REMOTE_HOOK = "#remote-hook:";
	public static final String ACTIVATED_HOST_HOOK = "#host-hook:";
	public static final String ACTIVATED_FINALIZER_HOOK = "#finally:";

	
	public static boolean isVanilaProp(String key) {
		return key.indexOf(':') < 0 && key.indexOf('#') < 0;
	}

	public static String getPragmaQualifier(String key) {
		int n = key.indexOf(':');
		if (n < 0) {
			return null;
		}
		else {
			return key.substring(0, n);
		}
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
	public Map<String, Object> getConfigMap() {
		return config;
	}

	@Override
	Object readRawProp(String name) {
		return config.get(name);
	}
	
	@Override
	void setRawProp(String name, Object value) {
		config.put(name, value);		
	}

	@Override
	@PropName(NODE_NAME)
	public String getNodeName() {
		return readString();
	}

	@Override
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
	
	@Override
	@PropName(SPI_CLOUD_CONTEXT)
	@DefaultNull
	public CloudContext getCloudContext() {
		return readObject();
	}
	
	@Override
	@PropName(SPI_CONTROL_CONSOLE)
	@DefaultNull
	public HostControlConsole getControlConsole() {
		return readObject();
	}

	@Override
	@PropName(SPI_PROCESS_LAUNCHER)
	@DefaultNull
	public ProcessLauncher getProcessLauncher() {
		return readObject();
	}

	@Override
	@PropName(SPI_REMOTING_SESSION)
	@DefaultNull
	public RemoteExecutionSession getRemotingSession() {
		return readObject();
	}
	
	@Override
	@PropName(SPI_JVM_EXEC_CMD)
	@DefaultNull
	public String getJvmExecCmd() {
		return readObject();
	}

	@Override
	@PropName(SPI_JVM_CLASSPATH)
	@DefaultNull
	public List<ClasspathEntry> getJvmClasspath() {
		return readObject();
	}
	
	@Override
	@PropName(SPI_JVM_ARGS)
	@DefaultNull
	public List<String> getJvmArgs() {
		return readObject();
	}
	
	@Override
	@PropName(SPI_MANAGED_PROCESS)
	@DefaultNull
	public ManagedProcess getManagedProcess() {
		return readObject();
	}
	
	@Override
	@PropName(SPI_NODE_FACTORY)
	@DefaultNull
	public NodeFactory getNodeFactory() {
		return readObject();
	}
	
	@Override
	@PropName(SPI_NODE_INSTANCE)
	@DefaultNull
	public ViNode getNodeInstance() {
		return readObject();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) config.get(key);
	}
	
	public static class Console extends Delegate {
		
		private ViConfigurable conf;

		public static Console at(ViConfigurable conf) {
			return new Console(conf);
		}
		
		public Console(ViConfigurable conf) {
			this.conf = conf;
		}

		@Override
		protected ViConfigurable getConfigurable() {
			return conf;
		}

		public Console write(String text) {
			// TODO not implemented
			return this;
		}

		public Console bindOut(OutputStream os) {
			conf.setConfigElement(CONSOLE_STD_OUT, os);
			return this;
		}

		public Console bindOut(Writer writer) {
			conf.setConfigElement(CONSOLE_STD_OUT, new WriterOutputStream(writer));
			return this;
		}

		public Console echoOut(boolean echo) {
			conf.setConfigElement(CONSOLE_STD_OUT_ECHO, String.valueOf(echo));
			return this;			
		}
		
		public Console bindErr(OutputStream os) {
			conf.setConfigElement(CONSOLE_STD_ERR, os);
			return this;			
		}

		public Console bindErr(Writer writer) {
			conf.setConfigElement(CONSOLE_STD_ERR, new WriterOutputStream(writer));
			return this;			
		}

		public Console echoErr(boolean echo) {
			conf.setConfigElement(CONSOLE_STD_ERR_ECHO, String.valueOf(echo));
			return this;						
		}

		public Console echoPrefix(String prefix) {
			conf.setConfigElement(CONSOLE_ECHO_PREFIX, prefix);
			return this;			
		}

		public Console flush() {
			conf.setConfigElement(CONSOLE_FLUSH, "");
			return this;						
		}

		public Console closeIn() {
			// TODO implement
			return this;						
		}

		public Console silentShutdown(boolean silent) {
			conf.setConfigElement(CONSOLE_SILENT_SHUTDOWN, String.valueOf(silent));
			return this;
		}
	}
}
