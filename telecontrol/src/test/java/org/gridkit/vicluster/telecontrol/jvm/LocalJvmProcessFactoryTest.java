package org.gridkit.vicluster.telecontrol.jvm;

import static org.hamcrest.CoreMatchers.not;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.ControlledProcess;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.junit.Assert;
import org.junit.Test;

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
		Assert.assertThat(rname, not(name));
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
		Assert.assertThat(rname, not(name));
		process.getProcess().destroy();
	}	


	@SuppressWarnings("serial")
	private final static class GetJvmName implements Callable<String>, Serializable {
		@Override
		public String call() throws Exception {
			return ManagementFactory.getRuntimeMXBean().getName();
		}
	}	
}
