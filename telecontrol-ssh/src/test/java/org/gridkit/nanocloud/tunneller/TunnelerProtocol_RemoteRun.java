package org.gridkit.nanocloud.tunneller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeSet;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.telecontrol.TunnellerProtocolTest;
import org.gridkit.vicluster.telecontrol.ssh.RemoteNodeProps;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TunnelerProtocol_RemoteRun {

	public static ViNodeSet cloud = CloudFactory.createSimpleSshCloud();
	public static ViNode host;
	
	@BeforeClass
	public static void initHost() {
		host = cloud.node("host");
		ViProps.at(host).setRemoteType();
		RemoteNodeProps.at(host).setRemoteHost("cbox1");
	}
	
	@Test
	public void ping() {
		host.exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Ping");				
			}
		});
	}

	@Test
	public void test_vanila_exec() {
		runTest("test_vanila_exec");
	}

	@Test
	public void test_exec_with_stdErr() {
		runTest("test_exec_with_stdErr");
	}
	
	@Test
	public void test_exec_with_redirect() {
		runTest("test_exec_with_redirect");
	}
	
	@Test
	public void test_exec_resource_leak() {
		runTest("test_exec_resource_leak");
	}
	
	// see original method
	public void test_bind() {
		runTest("test_bind");
	}
	
	@Test
	public void test_vanila_file_push() {
		runTest("test_vanila_file_push");
	}
	
	@Test
	public void test_user_home_file_push() {
		runTest("test_user_home_file_push");
	}
	
	@Test
	public void test_temp_dir_file_push() {
		runTest("test_temp_dir_file_push");
	}
	
	@Test
	public void test_mkdirs_on_file_push() {
		runTest("test_mkdirs_on_file_push");
	}
	
	@Test
	public void test_no_override() {
		runTest("test_no_override");
	}
	
	@Test
	public void test_error_handling() {
		runTest("test_error_handling");
	}
	
	@Test
	public void test_error_handling2() {
		runTest("test_error_handling2");
	}
	
	@Test
	public void test_error_handling3() {
		runTest("test_error_handling3");
	}
	
	@Test
	public void test_concurrent_write_handling() {
		runTest("test_concurrent_write_handling");
	}
	
	private void runTest(final String method) {
		host.exec(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					TunnellerProtocolTest test = new TunnellerProtocolTest();
					test.start();
					
					Method m = test.getClass().getMethod(method);
					m.invoke(test);
				} catch (SecurityException e) {
					throw new RuntimeException(e);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (TimeoutException e) {
					throw new RuntimeException(e);
				} catch (NoSuchMethodException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				} catch (InvocationTargetException e) {
					if (e.getCause() instanceof Error) {
						throw (Error)e.getCause();
					}
					else {
						throw new RuntimeException(e.getCause());
					}
				}
			}
		});
	}
	
	@AfterClass
	public static void shutdownHost() {
		cloud.shutdown();
	}
	
}
