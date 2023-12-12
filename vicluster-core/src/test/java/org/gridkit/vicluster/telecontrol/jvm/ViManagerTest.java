/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.vicluster.telecontrol.jvm;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.gridkit.vicluster.CompositeViNodeProvider;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNodeProvider;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.isolate.IsolateAwareNodeProvider;
import org.junit.After;
import org.junit.Test;

import junit.framework.Assert;

public class ViManagerTest {
	
	private ViManager manager = createViManager();
	
	public ViManager createViManager() {
		CompositeViNodeProvider provider = new CompositeViNodeProvider();

		ViNodeProvider isolateProvider = new IsolateAwareNodeProvider();
		Map<String, String> isolateSelector = new HashMap<String, String>();
		isolateSelector.put(ViProps.NODE_TYPE, "isolate");
		provider.addProvider(isolateSelector, isolateProvider);

		ViNodeProvider localProvider = new JvmNodeProvider(new LocalJvmProcessFactory(BackgroundStreamDumper.SINGLETON));
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
