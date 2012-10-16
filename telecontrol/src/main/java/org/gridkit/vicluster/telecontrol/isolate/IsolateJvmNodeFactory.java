package org.gridkit.vicluster.telecontrol.isolate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.telecontrol.ExecCommand;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.zerormi.hub.RemotingEndPoint;

public class IsolateJvmNodeFactory extends LocalJvmProcessFactory {

	private String[] isolatedPackages;

	public IsolateJvmNodeFactory(String... packages) {
		this.isolatedPackages = packages;
	}
	
	@Override
	protected Process startProcess(String name, ExecCommand jvmCmd) throws IOException {
		return new IsolateProcess(name, isolatedPackages, jvmCmd);
	}
	
	
	private static class IsolateProcess extends Process {

		private Isolate isolate;
		private CountDownLatch down = new CountDownLatch(1);
		
		private ByteArrayOutputStream stdIn = new ByteArrayOutputStream();
		private ByteArrayInputStream stdOut = new ByteArrayInputStream(new byte[0]);
		private ByteArrayInputStream stdErr = new ByteArrayInputStream(new byte[0]);
		
		public IsolateProcess(String name, String[] isolatedPackages, final ExecCommand jvmCmd) {
			isolate = new Isolate(name, isolatedPackages);
			isolate.start();
			isolate.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					start(jvmCmd);
					return null;
				}
			});
		}

		private static void start(ExecCommand jvmCmd) throws Exception {
			System.setProperty(RemotingEndPoint.HEARTBEAT_PERIOD, String.valueOf(Integer.MAX_VALUE));
			System.setProperty(RemotingEndPoint.HEARTBEAT_TIMEOUT, String.valueOf(Integer.MAX_VALUE));
			
			System.out.println("Starting: " + jvmCmd.getCommand());
			try {
				String main = null;
				List<String> args = new ArrayList<String>();
				boolean skip = false;
				
				for(String opt: jvmCmd.getArguments()) {
					if (main != null) {
						args.add(opt);
					}
					else {
						if (skip) {
							skip = false;
							continue;
						}
						if (opt.startsWith("-D")) {
							int n = opt.indexOf('=');
							if (n > 0) {
								String prop = opt.substring(2, n);
								String value = opt.substring(n + 1);
								System.setProperty(prop,  value);
							}
						}
						else if (opt.startsWith("-cp")) {
							skip = true;
						}
						else if (opt.startsWith("-")) {
							System.err.println("Ignore arg: " + opt);
						}
						else {
							main = opt;
						}
					}
				}
				
				Class<?> c = Class.forName(main);
				Method m = c.getMethod("main", String[].class);
				m.setAccessible(true);
				m.invoke(0, new Object[]{args.toArray(new String[0])});
			}
			catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		}
		
		
		@Override
		public OutputStream getOutputStream() {
			return stdIn;
		}

		@Override
		public InputStream getInputStream() {
			return stdOut;
		}

		@Override
		public InputStream getErrorStream() {
			return stdErr;
		}

		@Override
		public int waitFor() throws InterruptedException {
			down.await();
			return 0;
		}

		@Override
		public int exitValue() {
			if (down.getCount() == 0) {
				return 0;
			}
			else {
				throw new IllegalThreadStateException("running");
			}
		}

		@Override
		public synchronized void destroy() {
			if (down.getCount() > 0) {
				isolate.stop();
				down.countDown();
			}
		}
	}
}
