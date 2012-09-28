package org.gridkit.vicluster;

import java.io.File;
import java.io.FileNotFoundException;
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

import org.gridkit.vicluster.isolate.IsolateViNodeProvider;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.gridkit.vicluster.telecontrol.ssh.ConfigurableSshSessionProvider;
import org.gridkit.vicluster.telecontrol.ssh.SimpleSshJvmReplicator;
import org.gridkit.vicluster.telecontrol.ssh.SimpleSshSessionProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ViManagerCheck {

	
	private ViManager manager; 
	
	@Before
	public void initCloud() throws IOException {
		manager = createViManager();
	}
	
	public ViManager createViManager() throws IOException {
		CompositeViNodeProvider provider = new CompositeViNodeProvider();
		
		ViNodeProvider isolateProvider = new IsolateViNodeProvider();
		Map<String, String> isolateSelector = new HashMap<String, String>();
		isolateSelector.put(ViProps.NODE_TYPE, "isolate");
		provider.addProvider(isolateSelector, isolateProvider);

		ViNodeProvider localProvider = new JvmNodeProvider(new LocalJvmProcessFactory());
		Map<String, String> localSelector = new HashMap<String, String>();
		localSelector.put(ViProps.NODE_TYPE, "clone-jvm");
		provider.addProvider(localSelector, localProvider);

		Properties props = new Properties();
		File conf = new File(new File(System.getProperty("user.home")), "ssh-credentials.prop"); 
		FileReader fr = new FileReader(conf);
		props.load(fr);
		fr.close();
		
		ConfigurableSshSessionProvider sshFactory = new ConfigurableSshSessionProvider(props);
		SimpleSshJvmReplicator replicator = new SimpleSshJvmReplicator("longmrdfappd1.uk.db.com", null, sshFactory);
//		SimpleSshJvmReplicator replicator = new SimpleSshJvmReplicator("longmdcfu531.uk.db.com", null, sshFactory);
		replicator.setJavaExecPath("/apps/grimis/java/linux/jdk1.6.0_22/bin/java");

		replicator.setAgentHome("/tmp/.gridagent2");
		try {
			replicator.init();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		ViNodeProvider sshProvider = new JvmNodeProvider(replicator);
		Map<String, String> sshSelector = new HashMap<String, String>();
		localSelector.put(ViProps.NODE_TYPE, "ssh-clone-jvm");
		provider.addProvider(sshSelector, sshProvider);
		
		return new ViManager(provider);
	}
	
	@After
	public void dropNodes() {
		manager.shutdown();
	}
	
	@Test
	public void test_ssh_node() {
		manager.node("jvm.remote.**").setProp(ViProps.NODE_TYPE, "ssh-clone-jvm");
		
		manager.node("jvm.remote.node1");
		
		List<String> ids = manager.node("**.node1").massExec(new Callable<String>(){
			@Override
			public String call() throws Exception {
				System.out.println("This is std out");
				System.err.println("This is std err");
				return ManagementFactory.getRuntimeMXBean().getName();
			}
		});
		ids = new ArrayList<String>(ids);
		
		String name = ManagementFactory.getRuntimeMXBean().getName();
		System.out.println("Local JVM: " + name + " Nodes' JVM: " + ids);		
	}

	@Test
	public void test_bulk_ssh_nodes() {
		manager.node("jvm.remote.**").setProp(ViProps.NODE_TYPE, "ssh-clone-jvm");

		for(int i = 0; i != 50; ++i) {
			manager.node("jvm.remote.node" + i);
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
	public void test_isolate_local_remote_node() {
		
		manager.node("isolate.**").setProp(ViProps.NODE_TYPE, "isolate");
		manager.node("jvm.local.**").setProp(ViProps.NODE_TYPE, "clone-jvm");
		manager.node("jvm.remote.**").setProp(ViProps.NODE_TYPE, "ssh-clone-jvm");
		
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
