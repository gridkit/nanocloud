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

import junit.framework.Assert;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViNodeConfig;
import org.gridkit.vicluster.telecontrol.LocalJvmProcessFactory;
import org.gridkit.vicluster.telecontrol.jvm.JvmNodeProvider;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.junit.After;
import org.junit.Test;

public class BasicJvmNodeTest {

	LocalJvmProcessFactory lpf = new LocalJvmProcessFactory();
	JvmNodeProvider nfactory = new JvmNodeProvider(lpf);

	@After
	public void cleanup() {
		lpf.stop();
	}
	
	@Test
	public void hallo_world_test() {
		
		ViNode node = nfactory.createNode("HalloWelt", new ViNodeConfig());
		
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
		
		ViNodeConfig config = new ViNodeConfig();
		
		JvmProps.addJvmArg(config, "-Dtest-property=Y-a-a-hoo");
		
		ViNode node = nfactory.createNode("HalloWelt", config);
		
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
		
		ViNodeConfig config = new ViNodeConfig();
		
		JvmProps.addJvmArg(config, "|-Dtest-property1=Y-a-a-hoo|-Dtest-property2=Boo");
		
		ViNode node = nfactory.createNode("HalloWelt", config);
		
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
		
		ViNodeConfig config = new ViNodeConfig();
		
		JvmProps.addJvmArg(config, "-XX:+InvalidOption");
		
		ViNode node = nfactory.createNode("HalloWelt", config);
		
		node.exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Ping");
			}
		});
		
		
		node.shutdown();
	}	
}
