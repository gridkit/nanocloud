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
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.vicluster.MassExec;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.ViNodeConfig.ReplyProps;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.telecontrol.ManagedProcess;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class JvmNode implements ViNode {

	private String name;
	private ManagedProcess process;
	private AdvancedExecutor executor;
	
	private WrapperPrintStream stdOut;
	private WrapperPrintStream stdErr;
	
	private ViNodeConfig config = new ViNodeConfig();
	
	private boolean active;
	
	public JvmNode(String name, ViNodeConfig config, ManagedProcess cp) throws IOException {
		this.name = name;
		this.process = cp;
		this.executor = cp.getExecutionService();
		
		config.apply(this.config);
		
		stdOut = new WrapperPrintStream("[" + name + "] ", System.out, true);
		stdErr = new WrapperPrintStream("[" + name + "] ", System.err, true);

		cp.bindStdIn(null);
		cp.bindStdOut(stdOut);
		cp.bindStdErr(stdErr);
		
		initPropperteis();
		runStartupHooks();
		active = true;
	}

	private void initPropperteis() throws IOException {

		final Map<String, String> props = new HashMap<String, String>();
		props.put("vinode.name", name);
		ReplyProps replay = new ReplyProps() {
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

	private void runStartupHooks() throws IOException {
		try {
			ViEngine.Core.processStartupHooks(config, executor);
		}
		catch(Exception e) {
			throw new IOException("Node '" + name + "' has failed to initialize", e);
		}
	}
	
	private void runShutdownHooks() throws IOException {
		try {
			ViEngine.Core.processShutdownHooks(config, executor);
		}
		catch(Exception e) {
			throw new IOException("Node '" + name + "' has failed to initialize", e);
		}
	}

	@Override
	public void touch() {
		ensureStarted();
	}

	@Override
	public void exec(Runnable task) {
		try {
			submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			ExceptionHelper.throwUnchecked(e);
		}
	}

	@Override
	public void exec(VoidCallable task) {
		try {
			submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			ExceptionHelper.throwUnchecked(e);
		}
	}

	@Override
	public <T> T exec(Callable<T> task) {
		try {
			return submit(task).get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			ExceptionHelper.throwUnchecked(e);
			throw new Error("Unreachable");			
		}
	}

	@Override
	public Future<Void> submit(Runnable task) {
		ensureStarted();
		return executor.submit(task);
	}

	@Override
	public Future<Void> submit(VoidCallable task) {
		ensureStarted();
		return executor.submit(new VoidCallable.VoidCallableWrapper(task));
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		ensureStarted();
		return executor.submit(task);
	}

	@Override
	public <T> List<T> massExec(Callable<? extends T> task) {
		return MassExec.singleNodeMassExec(this, task);
	}

	@Override
	public List<Future<Void>> massSubmit(Runnable task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}

	@Override
	public List<Future<Void>> massSubmit(VoidCallable task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}

	@Override
	public <T> List<Future<T>> massSubmit(Callable<? extends T> task) {
		return MassExec.singleNodeMassSubmit(this, task);
	}

	private synchronized void ensureStarted() {
		if (!active) {
			throw new IllegalStateException("Node '" + name + "' is not active");
		}
	}

	@Override
	public void setProp(final String propName, final String value) {
		exec(new Runnable(){
			@Override
			public void run() {
				System.setProperty(propName, value);
			}
		});
	}

	@Override
	public void setProps(Map<String, String> props) {
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
		if (value instanceof String || value == null) {
			setProp(key, (String)value);
		}
		else if (key.startsWith("hook:")) {
			config.setConfigElement(key, value);
		}
		else {
			throw new Error("Not implemented: " + key);
		}
	}

	@Override
	public void setConfigElements(Map<String, Object> config) {
		for(String key: config.keySet()) {
			setConfigElement(key, config.get(key));
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
	@SuppressWarnings("deprecation")
	public synchronized void addShutdownHook(String name, Runnable hook, boolean override) {
		if (active) {
			config.addShutdownHook(name, hook, override);
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
		// TODO
	}

	@Override
	public void resume() {
		// TODO
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
			try {
				if (gracefully) { 
					runShutdownHooks();
				}
			} catch (IOException e) {
				e.printStackTrace(); // TODO logging
			}
			if ("TRUE".equals(config.getProp(ViProps.NODE_SILENT_SHUTDOWN, "false").toUpperCase())) {
				stdOut.silence();
				stdErr.silence();
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
		}		
	}

	private Runnable poisonPill(boolean graceful) {
		if (graceful) {
			return new Runnable() {
				@Override
				public void run() {
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
					if (System.getProperty("org.gridkit.suppress-system-exit") == null) {
						Runtime.getRuntime().halt(0);
					}
				}
			};			
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
		
		public synchronized void silence() {
			try {
				if (buffer.size() > 0) {
					dumpBuffer();
				}
			} catch (IOException e) {
				// ignore
			}
			silence = true;
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
			dumpBuffer();	
			if (!ignoreClose) {
				printStream.close();
			}
		}
	}	
}
