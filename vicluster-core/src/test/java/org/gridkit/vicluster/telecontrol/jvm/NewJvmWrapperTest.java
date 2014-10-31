package org.gridkit.vicluster.telecontrol.jvm;

import java.io.IOException;

import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.junit.After;
import org.junit.Test;

public class NewJvmWrapperTest {

	LocalJvmProcessFactory lpf = new LocalJvmProcessFactory(BackgroundStreamDumper.SINGLETON);

	@After
	public void cleanup() {
		lpf.stop();
	}

	@Test
	public void hallo_world_test() throws IOException, InterruptedException {
	
//		JvmConfig jconfig = new JvmConfig();
//		ManagedProcess proc = ((ManagedProcess)lpf.createProcess("test", jconfig));
//		
//		AdvExecutor2ViExecutor target = new AdvExecutor2ViExecutor(proc.getExecutionService());
//		ViEngineNode node = new ViEngineNode("test", config, proc);
//				
//		node.exec(new Runnable() {
//			@Override
//			public void run() {
//				System.out.println("Hallo world!");
//			}
//		});
//		
//		Thread.sleep(100);
//		
//		node.shutdown();
	}	

	@Test
	public void test_output_receiver() throws IOException, InterruptedException {
	
//		JvmConfig jconfig = new JvmConfig();
//		ManagedProcess proc = ((ManagedProcess)lpf.createProcess("test", jconfig));
//		
//		Map<String, Object> config = new HashMap<String, Object>();
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		config.put("console:stdOut", bos);
//		
//		ViEngineNode node = new ViEngineNode("test", config, proc);
//				
//		node.exec(new Runnable() {
//			@Override
//			public void run() {
//				System.out.println("Hallo world!");
//			}
//		});
//		
//		Thread.sleep(100);
//		node.shutdown();
//		
//		String text = new String(bos.toByteArray());
//		Assert.assertEquals("Hallo world!", text.trim());
	}	
	

	@Test
	public void test_shutdown_hook() throws IOException, InterruptedException {
	
//		JvmConfig jconfig = new JvmConfig();
//		ManagedProcess proc = ((ManagedProcess)lpf.createProcess("test", jconfig));
//		
//		Map<String, Object> config = new HashMap<String, Object>();
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		config.put("console:stdOut", bos);
//		config.put(ViConf.CONSOLE_SILENT_SHUTDOWN, "false");
//		
//		config.put("hook:test-shutdown-hook", new Hooks.ShutdownHook(new Runnable() {
//			@Override
//			public void run() {
//				System.out.println("Shutting down");
//				System.out.flush();
//				try {
//					Thread.sleep(500);
//				} catch (InterruptedException e) {
//					// ignore
//				}
//			}
//		}));
//		
//		ViEngineNode node = new ViEngineNode("test", config, proc);
//				
//		node.exec(new Runnable() {
//			@Override
//			public void run() {
//				System.out.println("Hallo world!");
//			}
//		});
//		
//		Thread.sleep(100);
//		node.shutdown();
//		
//		String text = new String(bos.toByteArray());
//		Assert.assertArrayEquals(new String[]{"Hallo world!", "Shutting down"}, text.split("[\r\n]+"));
	}
	
	@Test
	public void test_post_shutdown_hook() throws IOException, InterruptedException {
	
//		JvmConfig jconfig = new JvmConfig();
//		ManagedProcess proc = ((ManagedProcess)lpf.createProcess("test", jconfig));
//		
//		Map<String, Object> config = new HashMap<String, Object>();
//		ByteArrayOutputStream bos = new ByteArrayOutputStream();
//		config.put("console:stdOut", bos);
//		
//		final String[] box = new String[1];
//		
//		config.put("hook:test-shutdown-hook", new Hooks.PostShutdownHook(new Runnable() {
//			@Override
//			public void run() {
//				System.out.println("Node has been shutdown");
//				box[0] = "Terminated";
//				System.out.flush();				
//				try {
//					Thread.sleep(500);
//				} catch (InterruptedException e) {
//					// ignore
//				}
//			}
//		}));
//		
//		ViEngineNode node = new ViEngineNode("test", config, proc);
//				
//		node.exec(new Runnable() {
//			@Override
//			public void run() {
//				System.out.println("Hallo world!");
//			}
//		});
//		
//		Thread.sleep(100);
//		node.shutdown();
//		
//		String text = new String(bos.toByteArray());
//		Assert.assertArrayEquals(new String[]{"Hallo world!"}, text.split("[\r\n]+"));
//		Assert.assertEquals("Terminated", box[0]);
	}		
}
