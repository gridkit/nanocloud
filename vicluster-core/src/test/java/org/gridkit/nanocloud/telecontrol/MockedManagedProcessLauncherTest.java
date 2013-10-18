package org.gridkit.nanocloud.telecontrol;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.gridkit.vicluster.telecontrol.StreamPipe;
import org.gridkit.vicluster.telecontrol.bootstraper.SmartBootstraper;
import org.gridkit.zerormi.zlog.ZLogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MockedManagedProcessLauncherTest {

	static {
		System.setProperty("org.gridkit.suppress-system-exit", "true");
		System.setProperty(ZLogFactory.PROP_ZLOG_MODE, "slf4j");
	}
	
	private LocalControlConsole console;
	private RemoteExecutionSession session;
	
	@Before
	public void initConsole() {
		console = new LocalControlConsole() {

			@Override
			public Destroyable startProcess(String workingDir, String[] command, Map<String, String> environment, final ProcessHandler handler) {
				final StreamPipe pipeIn = new StreamPipe(4096);
				StreamPipe pipeOut = new StreamPipe(4096);
				
				handler.started(pipeIn.getOutputStream(), pipeOut.getInputStream(), pipeOut.getInputStream());
				
				Thread slave = new Thread() {
					@Override
					public void run() {
						int exitCode = 1;
						try {
							SmartBootstraper.start(pipeIn.getInputStream());
							exitCode = 0;
						}
						finally {
							handler.finished(exitCode);
						}
					}
				};
				
				slave.setName("slave");
				slave.start();
				
				return null;
			}
			
		};
		session = new ZeroRmiRemoteSession("test");
	}

	@After
	public void destroyConsole() {
		console.terminate();
		session.terminate();
	}

	@Test	
	public void startSlave() throws InterruptedException, ExecutionException {
		Map<String, Object> config = new HashMap<String, Object>();
		config.put("node:name", "test");
		config.put(ViConf.SPI_CONTROL_CONSOLE, console);
		config.put(ViConf.SPI_REMOTING_SESSION, session);
		config.put(ViConf.JVM_EXEC_CMD, new File(new File(System.getProperty("java.home"), "bin"), "java").getPath());
		config.put(ViConf.SPI_JVM_ARGS, new ArrayList<String>());
		config.put(ViConf.SPI_JVM_CLASSPATH, Classpath.getClasspath(Thread.currentThread().getContextClassLoader()));
		
		ProcessSporeLauncher launcher = new ProcessSporeLauncher();
		ManagedProcess slave = launcher.createProcess(config);
		
		slave.bindStdOut(System.out);
		slave.bindStdErr(System.err);
		
		slave.bindStdIn(new ByteArrayInputStream("Ping".getBytes()));
		
		slave.getExecutionService().submit(new Callable<Void>() {
			
			@Override
			public Void call() throws Exception {
//				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//				Assert.assertEquals("Ping", br.readLine());
//				Assert.assertNull(br.readLine());
				System.out.println("Ping check skipped");
				return null;
			}
		}).get();

		try {
			slave.destroy();
		} catch (Exception e) {
			// ignore
		}
		
		Assert.assertEquals((Integer)0, slave.getExitCodeFuture().get());
		
	}	
}
