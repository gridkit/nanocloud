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
package org.gridkit.vicluster.isolate.coherence;

import java.util.concurrent.Callable;

import org.gridkit.vicluster.ViGroup;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.isolate.IsolateViNode;
import org.gridkit.vicluster.isolate.coherence.CohHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class ViClusterSamplesTest {
	
	ViGroup cluster;
	
	@Test
	public void test_simple_cluster() {
		// ViGroup allows you to work with multiple ViNodes together
		// static variable is used to clean up all created ViNodes after test
		cluster = new ViGroup();

		// you should add to isolate package list
		// * Coherence (com.tangosol)
		// * GridKit (org.gridkit)
		// * Your application package
		// * Some libraries should also be included to work properly (e.g. mockito)
		IsolateViNode.includePackages(cluster, "org.gridkit", "com.tangosol");
		
		// preset for in-JVM cluster
		CohHelper.enableFastLocalCluster(cluster);
		// using default config in this case
		CohHelper.cacheConfig(cluster, "/coherence-cache-config.xml");

		// creating server node
		ViNode storage = new IsolateViNode("storage");
		// adding node to "cluster" group, group setting will apply to node
		cluster.addNode(storage);
		
		CohHelper.localstorage(storage, true);
		
		// simulating DefaultCacheServer startup
		CohHelper.startDefaultCacheServer(storage);

		// creating client node and adding it to a group
		ViNode client = new IsolateViNode("client");
		cluster.addNode(client);
		
		// disabling local storage for "client" node making it Coherence data client
		CohHelper.localstorage(client, false);

		final String cacheName = "distr-a";
		
		// pure magic at this point
		// instance of callable will be serialized and deserialized with classloader of "client" node
		// and executed in context of client JVM
		// you can think of it a of remote call
		// return values or raised exceptions are converted to application classloader on way back
		client.exec(new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				
				NamedCache cache = CacheFactory.getCache(cacheName);
				
				cache.put(0, 0);
				Assert.assertEquals(0, cache.get(0));
				
				return null;
			}
		});
	}
	
	@After
	public void tearDownIsolates() {
		// It is not recommended to shutdown cluster after each test because
		// cluster startup takes few seconds.
		// Normally you would setup your application topology once and resuse it
		// in multiple tests
		if (cluster != null) {
			cluster.shutdown();
			cluster = null;
		}
	}	
	
	@Test
	public void test_parameter_passing() {

		cluster = new ViGroup();
		IsolateViNode.includePackage(cluster, "org.gridkit");
		ViNode node = new IsolateViNode("node");
		cluster.addNode(node);
		
		
		final double doubleV = 1.1d;
		
		node.exec(new Callable<Void>(){
			@Override
			public Void call() throws Exception {

				// final local variable from outer scope can be accessed as usual
				Assert.assertEquals(1.1d, doubleV, 0d);
				return null;
			}
		});

		final double[] doubleA = new double[]{1.1d};
		
		node.exec(new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				
				Assert.assertEquals(1.1d, doubleA[0], 0d);
				
				// this will not be visible to caller, 
				// code inside of isolate is working with copy of array				
				doubleA[0] = 2.2d;
				return null;
			}
		});

		// array is outer scope there not changed
		Assert.assertEquals(1.1d, doubleA[0], 0d);
	}	

	void doSomething() {		
	}
	
	@Test(expected=NullPointerException.class)
	public void test_outter_methods_unaccessible() {

		cluster = new ViGroup();
		IsolateViNode.includePackage(cluster, "org.gridkit");
		ViNode node = new IsolateViNode("node");
		cluster.addNode(node);
				
		node.exec(new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				
				// this will cause NPE
				// instance to outer class were not passed to isolate
				// this limitation is intentional
				doSomething();

				return null;
			}
		});
	}	
}
