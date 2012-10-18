package org.gridkit.vicluster;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.vicluster.isolate.IsolateViNodeProvider;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class CloudFactoryCheck {

	private static String CONFIG = "~/nanocloud-testcluster.viconf";
	
	private ViManager manager; 

	@After
	public void dropNodes() {
		if (manager != null) {
			manager.shutdown();
		}
	}
	
	@Before
	public void checkConfig() {
		Assume.assumeTrue(new File(new File(System.getProperty("user.home")), CONFIG.substring(2)).exists());
	}
	
	@Test
	public void test_ssh_node() throws InterruptedException {
		
		manager = CloudFactory.createSshCloud(CONFIG);
		
		manager.node("jvm.remote.**").setProp(ViProps.NODE_TYPE, "remote");
		manager.node("jvm.remote.host1");
		
		List<String> ids = manager.node("**.host1").massExec(new Callable<String>(){
			@Override
			public String call() throws Exception {
				System.out.println("This is std out");
				System.err.println("This is std err");
				Thread.sleep(500);
				return ManagementFactory.getRuntimeMXBean().getName();
			}
		});
		ids = new ArrayList<String>(ids);
		
		String name = ManagementFactory.getRuntimeMXBean().getName();
		System.out.println("Local JVM: " + name + " Nodes' JVM: " + ids);
		
		Thread.sleep(500);
	}

	@Test
	public void test_ssh_forced_local() throws InterruptedException {
		
		manager = CloudFactory.createLocalCloud(CONFIG);
		
		manager.node("jvm.remote.**").setProp(ViProps.NODE_TYPE, "remote");
		manager.node("jvm.remote.host1");
		
		List<String> ids = manager.node("**.host1").massExec(new Callable<String>(){
			@Override
			public String call() throws Exception {
				System.out.println("This is std out");
				System.err.println("This is std err");
				Thread.sleep(500);
				return ManagementFactory.getRuntimeMXBean().getName();
			}
		});
		ids = new ArrayList<String>(ids);
		
		String name = ManagementFactory.getRuntimeMXBean().getName();
		System.out.println("Local JVM: " + name + " Nodes' JVM: " + ids);
		
		Thread.sleep(500);
	}
	
	@Test
	public void test_bulk_ssh_nodes() {
		manager = CloudFactory.createSshCloud(CONFIG);
		
		manager.node("jvm.remote.**").setProp(ViProps.NODE_TYPE, "remote");

		for(int i = 0; i != 15; ++i) {
			manager.node("jvm.remote.host" + (1 + i % 3) + ".node" + i);
		}
		
		List<String> ids = manager.node("**.node*").massExec(new Callable<String>(){
			@Override
			public String call() throws Exception {
				return ManagementFactory.getRuntimeMXBean().getName();
			}
		});
		ids = new ArrayList<String>(ids);
		
		String name = ManagementFactory.getRuntimeMXBean().getName();
		System.out.println("Local JVM: " + name);
		System.out.println("Remote VMs:");
		for(String vmname: ids) {
			System.out.println("  " + vmname);
		}
	}
	
	@Test
	public void test_isolate_and_local_node() {
		manager = CloudFactory.createSshCloud(CONFIG);
		
		manager.node("isolate.**").setProp(ViProps.NODE_TYPE, "isolate");
		manager.node("jvm.local.**").setProp(ViProps.NODE_TYPE, "local");
		
		manager.node("isolate.node1");
		manager.node("jvm.local.node1");
		
		List<String> ids = manager.node("**.node1").massExec(new Callable<String>(){
			@Override
			public String call() throws Exception {
				String vmname = ManagementFactory.getRuntimeMXBean().getName();
				System.out.println("Hi, this JVM is named '" + vmname + "'");
				return vmname;
			}
		});
		ids = new ArrayList<String>(ids);
		
		String name = ManagementFactory.getRuntimeMXBean().getName();
		System.out.println("Local JVM: " + name + " Nodes' JVM: " + ids);
		
		Assert.assertEquals(2, ids.size());
		Assert.assertTrue("One of VM name should be same as this", ids.remove(name));
		Assert.assertFalse("Remaining VM name should be different", ids.remove(name));
	}

	@Test
	public void test_isolate_local_remote_node() {
		manager = CloudFactory.createSshCloud(CONFIG);
		
		manager.node("isolate.**").setProp(ViProps.NODE_TYPE, "isolate");
		manager.node("jvm.local.**").setProp(ViProps.NODE_TYPE, "local");
		manager.node("jvm.remote.**").setProp(ViProps.NODE_TYPE, "remote");
		
		manager.node("isolate.node1");
		manager.node("jvm.local.node1");
		manager.node("jvm.remote.host1.node1");
		
		List<String> ids = manager.node("**.node1").massExec(new Callable<String>(){
			@Override
			public String call() throws Exception {
				String vmname = ManagementFactory.getRuntimeMXBean().getName();
				System.out.println("HI! this JVM is named '" + vmname + "'");
				return vmname;
			}
		});
		ids = new ArrayList<String>(ids);
		
		String name = ManagementFactory.getRuntimeMXBean().getName();
		System.out.println("Local JVM: " + name);
		System.out.println("Other JVMs " + ids);
	}
}
