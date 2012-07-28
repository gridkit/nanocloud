package org.gridkit.gatling.remoting;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LocalJvmProcessFactoryTest {

	@Test
	public void test_echo() throws IOException, InterruptedException, ExecutionException {
		
		LocalJvmProcessFactory factory = new LocalJvmProcessFactory();
		
		ControlledProcess process = factory.createProcess(new JvmConfig());
		
		Process p = process.getProcess();
		p.getOutputStream().close();
		BackgroundStreamDumper.link(p.getInputStream(), System.out);
		BackgroundStreamDumper.link(p.getErrorStream(), System.err);

		String name = ManagementFactory.getRuntimeMXBean().getName();
		Future<String> f = process.getExecutionService().submit(new GetJvmName());
		String rname = f.get();
		System.out.println("VM names: " + name + " / " + rname);
		Assert.assertNotEquals(rname, name);
		process.getProcess().destroy();
	}	

	@Test
	public void test_annon_echo() throws IOException, InterruptedException, ExecutionException {
		
		LocalJvmProcessFactory factory = new LocalJvmProcessFactory();
		
		ControlledProcess process = factory.createProcess(new JvmConfig());
		
		String name = ManagementFactory.getRuntimeMXBean().getName();
		Future<String> f = process.getExecutionService().submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return ManagementFactory.getRuntimeMXBean().getName();
			}			
		});
		
		String rname = f.get();
		System.out.println("VM names: " + name + " / " + rname);
		Assert.assertNotEquals(rname, name);
		process.getProcess().destroy();
	}	


	@SuppressWarnings("serial")
	private final static class GetJvmName implements Callable<String>, Serializable {
		@Override
		public String call() throws Exception {
			return ManagementFactory.getRuntimeMXBean().getName();
		}
	}	

//	@SuppressWarnings("serial")
//	private final static class GetWorkDir implements Callable<String>, Serializable {
//		@Override
//		public String call() throws Exception {
//			return new File(".").getAbsoluteFile().getCanonicalPath();
//		}
//	}	
}
