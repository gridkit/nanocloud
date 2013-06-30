package org.gridkit.nanocloud.isolate.test;

import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.isolate.Isolate;
import org.gridkit.vicluster.isolate.IsolateProps;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IsolateNodeTest {

	
	private ViManager cloud;
	
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
		.isolatePackage("org.gridkit.nanocloud.isolate.test");
		
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
		Assert.assertTrue(cl.getClass().getName().startsWith(Isolate.class.getName()));
	}

	private static void assertShared(Class<?> c) {
		ClassLoader cl = c.getClassLoader();
		Assert.assertFalse(cl.getClass().getName().startsWith(Isolate.class.getName()));
	}	
}
