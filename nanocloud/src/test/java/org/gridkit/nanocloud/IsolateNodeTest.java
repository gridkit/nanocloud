/**
 * Copyright 2013 Alexey Ragozin
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
package org.gridkit.nanocloud;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.isolate.IsolateProps;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IsolateNodeTest {

	
	private Cloud cloud;
	
	@Before
	public void initCloud() {
		cloud = CloudFactory.createCloud();
		ViProps.at(cloud.node("**")).setIsolateType();
	}
	
	@After
	public void shutdownCloud() {
		cloud.shutdown();
	}
	
	@Test
	public void verify_default_isolation() {
		cloud.node("node1").exec(new Runnable() {
			@Override
			public void run() {
				assertIsolated(IsolateNodeTest.class);				
				assertIsolated(SharedStatics.class);				
			}
		});
	}

	@Test
	public void verify_package_sharing() {
		IsolateProps.at(cloud.node("node1"))
			.sharePackage("org.gridkit");
		
		cloud.node("node1").exec(new Runnable() {
			@Override
			public void run() {
				assertShared(IsolateNodeTest.class);				
				assertShared(SharedStatics.class);				
			}
		});
	}

	@Test
	public void verify_class_sharing() {
		IsolateProps.at(cloud.node("node1"))
		.shareClass(SharedStatics.class);
		
		cloud.node("node1").exec(new Runnable() {
			@Override
			public void run() {
				assertIsolated(IsolateNodeTest.class);				
				assertShared(SharedStatics.class);				
			}
		});
	}

	@Test
	public void verify_package_isolation() {
		IsolateProps.at(cloud.node("node1"))
		.sharePackage("")
		.isolatePackage("org.gridkit.nanocloud");
		
		cloud.node("node1").exec(new Runnable() {
			@Override
			public void run() {
				assertIsolated(IsolateNodeTest.class);				
				assertIsolated(SharedStatics.class);				
				assertShared(ViNode.class);				
			}
		});
	}

	private static void assertIsolated(Class<?> c) {
		ClassLoader cl = c.getClassLoader();
		Assert.assertTrue("Class " + c.getSimpleName() + " is expected to be isolated", cl.getClass().getName().startsWith(Isolate.class.getName()));
	}

	private static void assertShared(Class<?> c) {
		ClassLoader cl = c.getClassLoader();
		Assert.assertFalse("Class " + c.getSimpleName() + " is expected to be shared", cl.getClass().getName().startsWith(Isolate.class.getName()));
	}	
}
