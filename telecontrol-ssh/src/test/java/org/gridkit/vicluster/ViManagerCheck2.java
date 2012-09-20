package org.gridkit.vicluster;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.gridkit.vicluster.telecontrol.ssh.ConfigurableCloud;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ViManagerCheck2 {

	
	private ViManager manager;
	
	@Before
	public void createViManager() throws IOException {
		
		ConfigurableCloud cloud = new ConfigurableCloud();
		
		manager = new ViManager(cloud);		
		ViHelper.configure(manager, "cloud-check.prop");
	}
	
	@After
	public void dropNodes() {
		manager.shutdown();
	}
	
	@Test
	public void test_ssh_node() {
		manager.node("isolate.**").setProp(ViProps.NODE_TYPE, ViProps.NODE_TYPE_ISOLATE);
		manager.node("jvm.remote.**").setProp(ViProps.NODE_TYPE, ViProps.NODE_TYPE_REMOTE);
		
		manager.node("jvm.remote.node1");
		manager.node("jvm.remote.node2");
		
		List<String> ids = manager.node("**.node2").massExec(new Callable<String>(){
			@Override
			public String call() throws Exception {
				return ManagementFactory.getRuntimeMXBean().getName();
			}
		});
		ids = new ArrayList<String>(ids);
		
		String name = ManagementFactory.getRuntimeMXBean().getName();
		System.out.println("Local JVM: " + name + " Nodes' JVM: " + ids);		
	}
	
	@Test
	public void test_isolate_and_local_node() {
		
		manager.node("isolate.**").setProp(ViProps.NODE_TYPE, ViProps.NODE_TYPE_ISOLATE);
		manager.node("jvm.local.**").setProp(ViProps.NODE_TYPE, ViProps.NODE_TYPE_LOCAL);
		
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
		
		manager.node("isolate.**").setProp(ViProps.NODE_TYPE, ViProps.NODE_TYPE_ISOLATE);
		manager.node("jvm.local.**").setProp(ViProps.NODE_TYPE, ViProps.NODE_TYPE_LOCAL);
		manager.node("jvm.remote.**").setProp(ViProps.NODE_TYPE, ViProps.NODE_TYPE_REMOTE);
		
		manager.node("isolate.node1");
		manager.node("jvm.local.node1");
		manager.node("jvm.remote.node1");
		
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
