/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.vicluster.telecontrol.jvm;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.util.concurrent.Box;
import org.gridkit.vicluster.AdvExecutor2ViExecutor;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViExecutor;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeConfig.ReplyVanilaProps;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.Interceptor;
import org.gridkit.vicluster.ViEngine.Phase;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class ProcessNode implements ViNode {

	private String name;
	private ManagedProcess process;
	private AdvancedExecutor executor;
	
	private ViNodeConfig config = new ViNodeConfig();
	
	private SplittingOutputStream outplex;
	private SplittingOutputStream errplex;

	private ViExecutor execProxy;
	
	private boolean active;
	
	public ProcessNode(String name, Map<String, Object> config, ManagedProcess process) throws IOException {
		this.name = name;
		this.process = process;
		this.executor = process.getExecutionService();
		
		this.config.setConfigElements(config);
		
		OutputStream stdOut = this.config.getConsoleStdOutEcho() ? new WrapperPrintStream("[" + name + "] ", System.out, true) : null;
		OutputStream stdErr = this.config.getConsoleStdErrEcho() ? new WrapperPrintStream("[" + name + "] ", System.err, true) : null;

		OutputStream outSink = this.config.getConsoleStdOut();
		OutputStream errSink = this.config.getConsoleStdErr();
		
		List<OutputStream> outs = new ArrayList<OutputStream>(2);
		if (stdOut != null) {
			outs.add(stdOut);
		}
		if (outSink != null) {
			outs.add(outSink);
		}
		
		outplex = new SplittingOutputStream(outs.toArray(new OutputStream[0]));

		List<OutputStream> errs = new ArrayList<OutputStream>(2);
		if (stdErr != null) {
			errs.add(stdErr);
		}
		if (errSink != null) {
			errs.add(errSink);
		}
		
		errplex = new SplittingOutputStream(errs.toArray(new OutputStream[0]));
		
		process.bindStdIn(this.config.getConsoleStdIn());
		process.bindStdOut(outplex);
		process.bindStdErr(errplex);
		
		execProxy = new ExecProxy(executor);
		
		ViEngine helper = new ViEngine();
		Map<String, Object> postInit = helper.processPhase(Phase.POST_INIT, this.config.getInternalConfigMap());

		initPropperteis();
		
		active = true;
		helper.executeHooks(execProxy, postInit, false);
		
		this.process.getExitCodeFuture().addListener(new Box<Integer>() {

			@Override
			public void setData(Integer data) {
				ProcessNode.this.config.setProp("runtime:exitCode", data.toString());
			}

			@Override
			public void setError(Throwable e) {
				// ignore
			}
		});
	}

	private void initPropperteis() throws IOException {

		final Map<String, String> props = new HashMap<String, String>();
		props.put("vinode.name", name);
		ReplyVanilaProps replay = new ReplyVanilaProps() {
			@Override
			protected void setPropInternal(String propName, String value) {
				if (propName.indexOf(':') < 0) {
					props.put(propName, value);
				}
			}
		};
		
		try {
			config.apply(replay);
			executor.submit(new Runnable() {
				@Override
				public void run() {
					for(String key: props.keySet()) {
						String val = props.get(key);
						if (val == null) {
							System.getProperties().remove(key);
						}
						else {
							System.getProperties().put(key, val);
						}
					}
				}
			}).get();
		}
		catch(Exception e) {
			throw new IOException("Node '" + name + "' has failed to initialize", e);
		}
	}

	private void processPreShutdown() {
		ViEngine helper = new ViEngine();
		Map<String, Object> phaseConfig = helper.processPhase(Phase.PRE_SHUTDOWN, this.config.getInternalConfigMap());
		helper.executeHooks(execProxy, phaseConfig, true);
	}

	private void processPostShutdown() {
		ViEngine helper = new ViEngine();
		Map<String, Object> phaseConfig = helper.processPhase(Phase.POST_SHUTDOWN, this.config.getInternalConfigMap());
		helper.executeHooks(execProxy, phaseConfig, true);		
	}
	
	@Override
	public void touch() {
		ensureStarted();
	}

	private synchronized void ensureStarted() {
		if (!active) {
			throw new IllegalStateException("Node '" + name + "' is not active");
		}
	}

	
	
	public void exec(Runnable task) {
		execProxy.exec(task);
	}

	public void exec(VoidCallable task) {
		execProxy.exec(task);
	}

	public <T> T exec(Callable<T> task) {
		return execProxy.exec(task);
	}

	public Future<Void> submit(Runnable task) {
		return execProxy.submit(task);
	}

	public Future<Void> submit(VoidCallable task) {
		return execProxy.submit(task);
	}

	public <T> Future<T> submit(Callable<T> task) {
		return execProxy.submit(task);
	}

	public <T> List<T> massExec(Callable<? extends T> task) {
		return execProxy.massExec(task);
	}

	public List<Future<Void>> massSubmit(Runnable task) {
		return execProxy.massSubmit(task);
	}

	public List<Future<Void>> massSubmit(VoidCallable task) {
		return execProxy.massSubmit(task);
	}

	public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
		return execProxy.massSubmit(task);
	}

	@Override
	public void setProp(final String propName, final String value) {
		setProps(Collections.singletonMap(propName, value));
	}

	@Override
	public void setProps(Map<String, String> props) {
		for(String p: props.keySet()) {
			if (!ViConf.isVanilaProp(p)) {
				throw new IllegalArgumentException("[" + p + "] is not 'vanila' prop");
			}
		}
		final Map<String, String> copy = new LinkedHashMap<String, String>(props);
		exec(new Runnable() {
			@Override
			public void run() {
				for(String name: copy.keySet()) {
					System.setProperty(name, copy.get(name));
				}
			}
		});
	}

	@Override
	public void setConfigElement(String key, Object value) {
		if (ViConf.isTemporary(key)) {
			throw new IllegalArgumentException("Shapd keys could be produced only by quorum games");
		}
		if (ViConf.isHook(key)) {
			ViEngine.Interceptor interceptor = (Interceptor) value;
			if (value != null) {
				interceptor.processAddHoc(key, this);
			}
			config.setConfigElement(key, value);
			return;
		}
		if (ViConf.isRuntime(key)) {
			throw new IllegalArgumentException("Runtime keys are read only");
		}
		if (ViConf.isNode(key)) {
			throw new IllegalArgumentException("Node properties cannot be modified after activation");
		}
		if (ViConf.isConsole(key)) {
			throw new IllegalArgumentException("Console keys cannot be modified after activation");
		}
		if (ViConf.isVanilaProp(key)) {
			setProps(Collections.singletonMap(key, (String)value));
			return;
		}
		throw new IllegalArgumentException("Unknown config element [" + key + "]");
	}

	@Override
	public void setConfigElements(Map<String, Object> config) {
		for(String key: config.keySet()) {
			setConfigElement(key, config);
		}
	}

	@Override
	public void addStartupHook(String name, Runnable hook) {
		throw new IllegalStateException("Node " + name + " is started already");
	}

	@Override
	public void addStartupHook(String name, Runnable hook, boolean override) {
		throw new IllegalStateException("Node " + name + " is started already");
	}

	@Override
	public synchronized void addShutdownHook(String name, Runnable hook) {
		if (active) {
			config.addShutdownHook(name, hook);
		}
	}

	@Override
	@Deprecated
	public synchronized void addShutdownHook(String name, Runnable hook, boolean override) {
		if (active) {
			config.addShutdownHook(name, hook);
		}
	}

	@Override
	public String getProp(final String propName) {
		// TODO handling special props
		return exec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return System.getProperty(propName);
			}
		});
	}

	@Override
	public void suspend() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resume() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void kill() {
		terminate(false);
	}

	@Override
	public void shutdown() {
		terminate(true);
	}

	protected synchronized void terminate(boolean gracefully) {
		if (active) {
			flushOutput();
			if (config.getSilenceOutputOnShutdown()) {
				outplex.silence();
				errplex.silence();
			}
			try {
				if (gracefully) { 
					processPreShutdown();
				}
			} catch (Exception e) {
				e.printStackTrace(); // TODO logging
			}
			boolean destroyDelay = false;
			try {
				Future<Void> f = submit(poisonPill(gracefully));
				try {
					f.get(100, TimeUnit.MILLISECONDS);
					destroyDelay = true;
					
				} catch (Exception e) {
					// it doesn't matter 
				}
			}
			catch(RejectedExecutionException e) {
				// ignore
			}
			if (destroyDelay) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore
				}
			}
			process.destroy();
			active = false;
			processPostShutdown();
		}		
	}

	private void flushOutput() {
		execProxy.exec(new Runnable() {
			@Override
			public void run() {
		          System.out.flush();
		          System.err.flush();
			}
		});
		process.consoleFlush();
	}

	private Runnable poisonPill(boolean graceful) {
		if (graceful) {
			return new Runnable() {
				@Override
				public void run() {
					System.out.flush();
					System.err.flush();
					if (System.getProperty("org.gridkit.suppress-system-exit") == null) {
						System.exit(0);
					}
				}
			};
		}
		else {
			return new Runnable() {
				@Override
				public void run() {
					System.out.flush();
					System.err.flush();
					if (System.getProperty("org.gridkit.suppress-system-exit") == null) {
						Runtime.getRuntime().halt(0);
					}
				}
			};
		}
	}

	private class ExecProxy extends AdvExecutor2ViExecutor {

		public ExecProxy(AdvancedExecutor advExec) {
			super(advExec);
		}

		@Override
		public void touch() {
			ensureStarted();
		}

		@Override
		protected AdvancedExecutor getExecutor() {
			ensureStarted();
			return super.getExecutor();
		}
	}
	
	// TODO make wrapper print stream shared utility class
	private static class WrapperPrintStream extends FilterOutputStream {

		private String prefix;
		private PrintStream printStream;
		private ByteArrayOutputStream buffer;
		private boolean ignoreClose;
		private boolean silence;
		
		public WrapperPrintStream(String prefix, PrintStream printStream, boolean ignoreClose) {
			super(printStream);
			this.prefix = prefix;
			this.printStream = printStream;
			this.buffer = new ByteArrayOutputStream();
			this.ignoreClose = ignoreClose;
		}
		
		private void dumpBuffer() throws IOException {
			if (!silence) {
				printStream.append(prefix);
				printStream.write(buffer.toByteArray());
				printStream.flush();
				buffer.reset();
			}
		}
		
		@Override
		public synchronized void write(int c) throws IOException {
			synchronized(printStream) {
				buffer.write(c);
				if (c == '\n') {
					dumpBuffer();
				}
			}
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			synchronized(printStream) {
				for (int i = 0; i != len; ++i) {
					if (b[off + i] == '\n') {
						writeByChars(b, off, len);
						return;
					}
				}
				buffer.write(b, off, len);
			}
		}

		private void writeByChars(byte[] cbuf, int off, int len) throws IOException {
			for (int i = 0; i != len; ++i) {
				write(cbuf[off + i]);
			}
		}

		@Override
		public void close() throws IOException {
			super.flush();
			if (buffer.size() > 0) {
				dumpBuffer();
			}
			if (!ignoreClose) {
				printStream.close();
			}
		}
	}	
}
