package org.gridkit.vicluster;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.NodeFactory;
import org.gridkit.nanocloud.telecontrol.ProcessLauncher;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSessionWrapper;
import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.ViConfigurable.Delegate;
import org.gridkit.vicluster.telecontrol.AgentEntry;
import org.gridkit.vicluster.telecontrol.Classpath.ClasspathEntry;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.StreamCopyService;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.gridkit.zeroio.WriterOutputStream;

public class ViConf extends GenericConfig implements ViSpiConfig {

	
	public static final String NODE_NAME = "node:name";
	public static final String NODE_TYPE = "node:type";
	public static final String NODE_TRACE = "node:config-trace";
	public static final String NODE_DUMP_ON_FAILURE = "node:config-dump-on-failure";
	
	public static final String NODE_TYPE__LOCAL = ViProps.NODE_TYPE_LOCAL;
	public static final String NODE_TYPE__ISOLATE = ViProps.NODE_TYPE_ISOLATE;
	public static final String NODE_TYPE__REMOTE = ViProps.NODE_TYPE_REMOTE;
	
	public static final String RUNTIME_PID = "runtime:pid";
	public static final String RUNTIME_HOST = "runtime:host";
	public static final String RUNTIME_EXIT_CODE = "runtime:exitCode";
	public static final String RUNTIME_EXIT_CODE_FUTURE = "runtime:exitCode.future";
	public static final String RUNTIME_EXECUTION_SUSPENDED = "runtime:suspended";

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
	public static final String JVM_WORK_DIR = "jvm:work-dir";
	public static final String JVM_ENV_VAR = JvmProps.JVM_ENV;
	public static final String JVM_AGENT = "jvm:agent";

	public static final String CLASSPATH_TWEAK = "classpath:tweak:";
    public static final String CLASSPATH_INHERIT = "classpath:inherit";
    public static final String CLASSPATH_USE_SHALLOW = "classpath:use-shallow";

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
	public static final String HOOK_AGENT_BUILDER = "hook:jvm-agent";

	public static final String PRAGMA_HANDLER = "pragma-handler:";
	public static final String PRAGMA_HANDLER__CONSOLE = "pragma-handler:console";
	public static final String PRAGMA_HANDLER__LOG = "pragma-handler:log";
	public static final String PRAGMA_HANDLER__HOOK = "pragma-handler:hook";
	public static final String PRAGMA_HANDLER__CLASSPATH = "pragma-handler:classpath";
	public static final String PRAGMA_HANDLER__JVM = "pragma-handler:jvm";
	public static final String PRAGMA_HANDLER__NODE = "pragma-handler:node";
	public static final String PRAGMA_HANDLER__RUNTIME = "pragma-handler:runtime";
	public static final String PRAGMA_HANDLER__REMOTE = "pragma-handler:remote";
	public static final String PRAGMA_HANDLER__REMOTE_RUNTIME = "pragma-handler:remote-runtime";
	
	public static final String TYPE_HANDLER = "type-handler:";

	public static final String SPI_CLOUD_CONTEXT = "#spi:cloud-context";
	public static final String SPI_STREAM_COPY_SERVICE = "#spi:stream-copy-service";
	public static final String SPI_KILL_SWITCH = "#spi:kill-switch";
	public static final String SPI_EPITAPH = "#spi:epitaph";
	public static final String SPI_INSTRUMENTATION_WRAPPER = "#spi:instrumentation_wrapper";
	public static final String SPI_INSTRUMENTATION_WRAPPER_APPLIED = "#spi:instrumentation_wrapper_applied";
	public static final String SPI_REMOTING_SESSION = "#spi:remoting-session";
	public static final String SPI_JVM_EXEC_CMD = JVM_EXEC_CMD; // TODO "#spi:jvm-exec-cmd";
	public static final String SPI_SLAVE_ARGS = "#spi:jvm-args";
	public static final String SPI_SLAVE_ENV = "#spi:slave-env";
	public static final String SPI_SLAVE_CLASSPATH = "#spi:jvm-classpath";
	public static final String SPI_SLAVE_SHALLOW_CLASSPATH = "#spi:shallow-jvm-classpath";
	public static final String SPI_CONTROL_CONSOLE = "#spi:control-console";
	public static final String SPI_PROCESS_LAUNCHER = "#spi:process-launcher";
	public static final String SPI_MANAGED_PROCESS = "#spi:managed-process";
	public static final String SPI_NODE_FACTORY = "#spi:node-factory";
	public static final String SPI_NODE_INSTANCE = "#spi:node-instance";
	public static final String SPI_LOGGER = "#spi:logger";
	public static final String SPI_SLAVE_AGENT = "#spi:jvm-agent";

	public static final String ERROR_NODE_BOOTSTRAP = "#error:bootstrap";
	
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

	@Override
	@PropName(NODE_TRACE)
	public boolean isConfigTraceEnbaled() {
	    return readBoolean();
	}	

	@Override
	@PropName(NODE_DUMP_ON_FAILURE)
	public boolean shouldDumpConfigOnFailure() {
	    return readBoolean();
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
	@PropName(SPI_STREAM_COPY_SERVICE)
	@DefaultNull
	public StreamCopyService getStreamCopyService() {
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
	@PropName(SPI_INSTRUMENTATION_WRAPPER)
	@DefaultNull
	public RemoteExecutionSessionWrapper getInstrumentationWrapper() {
		return readObject();
	}

	@Override
	@PropName(SPI_INSTRUMENTATION_WRAPPER_APPLIED)
	@DefaultNull
	public boolean isInstrumentationWrapperApplied() {		 
		return readString() == null ? false : readBoolean();
	}
	
	@Override
	@PropName(SPI_JVM_EXEC_CMD)
	@DefaultNull
	public String getJvmExecCmd() {
		return readObject();
	}

	@Override
	@PropName(SPI_SLAVE_SHALLOW_CLASSPATH)
	@DefaultNull
	public List<String> getSlaveShallowClasspath() {
		return readObject();
	}

	@Override
	@PropName(SPI_SLAVE_CLASSPATH)
	@DefaultNull
	public List<ClasspathEntry> getSlaveClasspath() {
		return readObject();
	}

	@Override
	@PropName(SPI_SLAVE_AGENT)
	@DefaultNull
	public List<AgentEntry> getSlaveAgents() {
		return readObject();
	}
	
	@Override
	@PropName(SPI_SLAVE_ARGS)
	@DefaultNull
	public List<String> getSlaveArgs() {
		return readObject();
	}

	@Override
	@PropName(SPI_SLAVE_ENV)
	@DefaultNull
	public Map<String, String> getSlaveEnv() {
		return readObject();
	}

	@Override
	@PropName(JVM_WORK_DIR)
	@DefaultNull
	public String getSlaveWorkDir() {
		return readString();
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
	
	public static class TypeConf extends Delegate {
		
		private ViConfigurable conf;

		public static TypeConf at(ViConfigurable conf) {
			return new TypeConf(conf);
		}
		
		public TypeConf(ViConfigurable conf) {
			this.conf = conf;
		}

		@Override
		protected ViConfigurable getConfigurable() {
			return conf;
		}
				
		public TypeConf setIsolate() {
			conf.setProp(NODE_TYPE, NODE_TYPE__ISOLATE);			
			return this;
		}

		public TypeConf setLocal() {
			conf.setProp(NODE_TYPE, NODE_TYPE__LOCAL);			
			return this;
		}

		public TypeConf setRemote() {
			conf.setProp(NODE_TYPE, NODE_TYPE__REMOTE);			
			return this;
		}
	}
	
	public static class ConsoleConf extends Delegate {
		
		private ViConfigurable conf;

		public static ConsoleConf at(ViConfigurable conf) {
			return new ConsoleConf(conf);
		}
		
		public ConsoleConf(ViConfigurable conf) {
			this.conf = conf;
		}

		@Override
		protected ViConfigurable getConfigurable() {
			return conf;
		}

		public ConsoleConf write(String text) {
			// TODO not implemented
			return this;
		}

		public ConsoleConf bindOut(OutputStream os) {
			conf.setConfigElement(CONSOLE_STD_OUT, os);
			return this;
		}

		public ConsoleConf bindOut(Writer writer) {
			conf.setConfigElement(CONSOLE_STD_OUT, new WriterOutputStream(writer));
			return this;
		}

		public ConsoleConf echoOut(boolean echo) {
			conf.setConfigElement(CONSOLE_STD_OUT_ECHO, String.valueOf(echo));
			return this;			
		}
		
		public ConsoleConf bindErr(OutputStream os) {
			conf.setConfigElement(CONSOLE_STD_ERR, os);
			return this;			
		}

		public ConsoleConf bindErr(Writer writer) {
			conf.setConfigElement(CONSOLE_STD_ERR, new WriterOutputStream(writer));
			return this;			
		}

		public ConsoleConf echoErr(boolean echo) {
			conf.setConfigElement(CONSOLE_STD_ERR_ECHO, String.valueOf(echo));
			return this;						
		}

		public ConsoleConf echoPrefix(String prefix) {
			conf.setConfigElement(CONSOLE_ECHO_PREFIX, prefix);
			return this;			
		}

		public ConsoleConf flush() {
			conf.setConfigElement(CONSOLE_FLUSH, "");
			return this;						
		}

		public ConsoleConf closeIn() {
			// TODO implement
			return this;						
		}

		public ConsoleConf silentShutdown(boolean silent) {
			conf.setConfigElement(CONSOLE_SILENT_SHUTDOWN, String.valueOf(silent));
			return this;
		}
	}
	
	public static class ClasspathConf extends Delegate {
		private static final AtomicInteger classPathTweakCounter = new AtomicInteger();

		private ViConfigurable conf;

		public static ClasspathConf at(ViConfigurable conf) {
			return new ClasspathConf(conf);
		}
		
		public ClasspathConf(ViConfigurable conf) {
			this.conf = conf;
		}

		@Override
		protected ViConfigurable getConfigurable() {
			return conf;
		}
		
		public ClasspathConf add(URL url) {
			return add(defaultName(url), url);
		}

		public ClasspathConf add(String path) {
			return add(pathToURL(path));
		}

		private URL pathToURL(String path) {
			try {
				File f = new File(path);
				return f.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		}

		public ClasspathConf add(String ruleName, URL url) {
			checkURL(url);
			conf.setProp(CLASSPATH_TWEAK + ruleName, "+" + urlToString(url));
			return this;
		}

		public ClasspathConf add(String ruleName, String path) {
			return add(ruleName, path);
		}
		
		public ClasspathConf remove(URL url) {			
			return remove(defaultName(url), url);
		}

		public ClasspathConf remove(String path) {
			return remove(pathToURL(path));
		}

		public ClasspathConf remove(String ruleName, URL url) {
			checkURL(url);
			conf.setProp(CLASSPATH_TWEAK + ruleName, "-" + urlToString(url));
			return this;
		}

		public ClasspathConf remove(String ruleName, String path) {
			return remove(ruleName, pathToURL(path));
		}

        public ClasspathConf inheritClasspath(boolean inherit){
            conf.setProp(CLASSPATH_INHERIT, String.valueOf(inherit));
            return this;
        }
        
        /**
         * In shallow mode, classpath string from master is used as is for slave JVM.
         * <br/>
         * This works only for non-remote slaves and incompatible with classpath tweaking.
         */
        public ClasspathConf useShallowClasspath(boolean shallow) {
            conf.setProp(CLASSPATH_USE_SHALLOW, String.valueOf(shallow));
            return this;        	
        }

		private String defaultName(URL url) {
			String name;
			name = urlToString(url);
			name = name.replace(':', '_');
			name = name.replace('/', '_');

			final int tweakNumber = classPathTweakCounter.incrementAndGet();
			final String tweakNumberStr = String.format("%010d", tweakNumber); // 10 digits will be enough for ordering any classpath

			return tweakNumberStr+"_"+name;
		}

		private String urlToString(URL url) {
			try {
				return url.toURI().toASCIIString();
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}
		
		private void checkURL(URL url) {
			if (!"file".equals(url.getProtocol())) {
				throw new IllegalArgumentException("Only file protocol is supporeted for classpath");
			}
		}
	}
	
	public static class JvmConf extends Delegate {
		private static AtomicLong AGENT_COUNTER = new AtomicLong();

		private ViConfigurable conf;

		public static JvmConf at(ViConfigurable conf) {
			return new JvmConf(conf);
		}
		
		public JvmConf(ViConfigurable conf) {
			this.conf = conf;
		}

		@Override
		protected ViConfigurable getConfigurable() {
			return conf;
		}
		
		public JvmConf setJavaExec(String... cmd) {
		    if (cmd.length == 0) {
		        throw new IllegalArgumentException("JVM command cannot be empty");
		    }
            StringBuilder sb = new StringBuilder();
            for(String arg: cmd) {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(arg);
            }
		    conf.setProp(JVM_EXEC_CMD, sb.toString());
		    return this;
		}
		
		public JvmConf addJvmArg(String string) {
			conf.setProp(JVM_ARGUMENT + "arg:" + string, string);
			return this;
		}

		public JvmConf addJvmArgs(String... args) {
			if (args.length == 0) {
				return this;
			}
			else if (args.length == 1) {
				addJvmArg(args[0]);
			}
			else {
				StringBuilder sb = new StringBuilder();
				for(String arg: args) {
					sb.append('|').append(arg);
				}
				addJvmArg(sb.toString());
			}
			return this;
		}

		public JvmConf addAgent(File agentJar, String options){
			conf.setProp(JVM_AGENT + AGENT_COUNTER.incrementAndGet(), agentJar.getAbsolutePath() + "=" + (options == null ? "" : options));
			return this;
		}

		public JvmConf addAgent(File agentJar){
			return addAgent(agentJar, null);
		}

		public JvmConf setWorkDir(String path) {
			conf.setProp(JVM_WORK_DIR, path);
			return this;
		}		

		public JvmConf setEnv(String name, String val) {
			conf.setProp(JVM_ENV_VAR + name, val == null ? "\00" : val);
			return this;
		}		
	}
	
	public static class HookConf extends Delegate {

		private static AtomicLong ANNON_COUNTER = new AtomicLong();
		
		private ViConfigurable conf;
		
		public static HookConf at(ViConfigurable conf) {
			return new HookConf(conf);
		}
		
		public HookConf(ViConfigurable conf) {
			this.conf = conf;
		}

		@Override
		protected ViConfigurable getConfigurable() {
			return conf;
		}
		
		public HookConf addStartupHook(Runnable hook) {
			setStartupHook("ANNONSTARTUP-" + ANNON_COUNTER.getAndIncrement(), hook);
			return this;
		}

		public HookConf setStartupHook(String id, Runnable hook) {
			String hn = ViConf.HOOK + id;
			setConfigElement(hn, new Hooks.StratupHook(hook));
			return this;
		}

		public HookConf addShutdownHook(Runnable hook) {
			setShutdownHook("ANNONSHUTDOWN-" + ANNON_COUNTER.getAndIncrement(), hook);
			return this;
		}
		
		public HookConf setShutdownHook(String id, Runnable hook) {
			String hn = ViConf.HOOK + id;
			setConfigElement(hn, new Hooks.ShutdownHook(hook));
			return this;
		}
		
		public HookConf addPostShutdownHook(Runnable hook) {
			setPostShutdownHook("ANNONSHUTDOWN-" + ANNON_COUNTER.getAndIncrement(), hook);
			return this;
		}
		
		public HookConf setPostShutdownHook(String id, Runnable hook) {
			String hn = ViConf.HOOK + id;
			setConfigElement(hn, new Hooks.PostShutdownHook(hook));
			return this;
		}
	}	
	
	public static class RuntimeEx extends ViNode.Delegate {

        public static RuntimeEx at(ViNode node) {
            return new RuntimeEx(node);
        }
        
        public RuntimeEx(ViNode node) {
            super(node);
        }
	    
        @Override
        protected ViNode getDelegate() {
            return super.getDelegate();
        }
	    
        @SuppressWarnings("unchecked")
		public FutureEx<Integer> exitCodeFuture() {
        	return (FutureEx<Integer>)getDelegate().getPragma(RUNTIME_EXIT_CODE_FUTURE);
        }

        public Integer exitCode() {
            String ec = getDelegate().getProp(RUNTIME_EXIT_CODE);
            return ec == null ? null : Integer.valueOf(ec);
        }	    

        public int join() {
        	try {
				FutureEx<Integer> ec = exitCodeFuture();
				return ec.get();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e.getCause());
			}
        }	    

        public void suspend() {
            throw new UnsupportedOperationException();
        }	    

        public void resume() {
            throw new UnsupportedOperationException();
        }	    
	}
}
