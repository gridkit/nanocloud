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

import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.telecontrol.BackgroundStreamDumper;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

public class BasicJvmNodeTest {

	ViManager cloud;

	@Before
	public void init() {
		cloud = new ViManager(new JvmNodeProvider(new LocalJvmProcessFactory(BackgroundStreamDumper.SINGLETON)));
	}

	@After
	public void cleanup() {
		cloud.shutdown();
	}
	
	@Test
	public void hallo_world_test() {
		
		ViNode node = cloud.node("HalloWelt");
		
		node.exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Hallo world!");
			}
		});
		
		node.shutdown();
	}	

	@Test
	public void test_single_VM_option() {
		
		ViNode node = cloud.node("HalloWelt");
		JvmProps.addJvmArg(node, "-Dtest-property=Y-a-a-hoo");
		
		node.exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertEquals("Y-a-a-hoo", System.getProperty("test-property"));
			}
		});

		Assert.assertEquals("Y-a-a-hoo", node.getProp("test-property"));
		
		node.shutdown();
	}	

	@Test
	public void test_multiple_VM_options() {
		
		ViNode node = cloud.node("HalloWelt");
		JvmProps.addJvmArg(node, "|-Dtest-property1=Y-a-a-hoo|-Dtest-property2=Boo");
		
		node.exec(new Runnable() {
			@Override
			public void run() {
				Assert.assertEquals("Y-a-a-hoo", System.getProperty("test-property1"));
				Assert.assertEquals("Boo", System.getProperty("test-property2"));
			}
		});
		
		Assert.assertEquals("Y-a-a-hoo", node.getProp("test-property1"));
		Assert.assertEquals("Boo", node.getProp("test-property2"));
		
		node.shutdown();
	}	

	@Test(expected=RuntimeException.class)
	public void test_invalid_VM_options() {
		
		ViNode node = cloud.node("HalloWelt");
		JvmProps.addJvmArg(node, "-XX:+InvalidOption");
		
		node.exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Ping");
			}
		});
		
		
		node.shutdown();
	}	
}
