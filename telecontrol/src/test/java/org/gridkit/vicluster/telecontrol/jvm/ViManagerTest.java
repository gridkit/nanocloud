package org.gridkit.vicluster.telecontrol.jvm;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.gridkit.vicluster.CompositeViNodeProvider;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.isolate.IsolateViNodeProvider;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.junit.After;
import org.junit.Test;

public class ViManagerTest {
	
	private ViManager manager = createViManager();
	
	public ViManager createViManager() {
		CompositeViNodeProvider provider = new CompositeViNodeProvider();
		
		ViNodeProvider isolateProvider = new IsolateViNodeProvider();
		Map<String, String> isolateSelector = new HashMap<String, String>();
		isolateSelector.put(ViProps.NODE_TYPE, "isolate");
		provider.addProvider(isolateSelector, isolateProvider);

		ViNodeProvider localProvider = new JvmNodeProvider(new LocalJvmProcessFactory());
		Map<String, String> localSelector = new HashMap<String, String>();
		localSelector.put(ViProps.NODE_TYPE, "clone-jvm");
		provider.addProvider(localSelector, localProvider);
		
		return new ViManager(provider);
	}
	
	@After
	public void dropNodes() {
		manager.shutdown();
	}
	
	@Test
	public void test_ssh_node() {
		manager.node("isolate.**").setProp(ViProps.NODE_TYPE, "isolate");
		manager.node("jvm.remote.**").setProp(ViProps.NODE_TYPE, "ssh-clone-jvm");
		
		manager.node("jvm.remote.node1");
		
		List<String> ids = manager.node("**.node1").massExec(new Callable<String>(){
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
		
		manager.node("isolate.**").setProp(ViProps.NODE_TYPE, "isolate");
		manager.node("jvm.local.**").setProp(ViProps.NODE_TYPE, "clone-jvm");
		
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
	public void test_parallel_local_nodes() {
		
		manager.node("jvm.local.**").setProp(ViProps.NODE_TYPE, "clone-jvm");
		
		manager.node("jvm.local.node1");
		manager.node("jvm.local.node2");
		manager.node("jvm.local.node3");
		manager.node("jvm.local.node4");
		manager.node("jvm.local.node5");
		
		// JVM should be started in parallel
		// unfortunately it is hardly possible to verify
		Collection<String> ids = manager.node("**").massExec(new Callable<String>(){
			@Override
			public String call() throws Exception {
				String vmname = ManagementFactory.getRuntimeMXBean().getName();
				System.out.println("Hi, this JVM is named '" + vmname + "'");
				return vmname;
			}
		});
		ids = new TreeSet<String>(ids);
		
		String name = ManagementFactory.getRuntimeMXBean().getName();
		System.out.println("Local JVM: " + name + " Nodes' JVM: " + ids);
		
		Assert.assertEquals(5, ids.size());
	}

}
