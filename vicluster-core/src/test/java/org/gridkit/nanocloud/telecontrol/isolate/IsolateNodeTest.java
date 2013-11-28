package org.gridkit.nanocloud.telecontrol.isolate;

import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.gridkit.nanocloud.telecontrol.LocalNodeTypeHandler;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViConf.Console;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.telecontrol.jvm.ViEngineNodeProvider;
import org.junit.Test;

public class IsolateNodeTest {

	@Test
	public void vi_engine_node_test() throws InterruptedException {
		ViManager cloud = new ViManager(new ViEngineNodeProvider());
		
		cloud.node("**").setConfigElement(ViConf.TYPE_HANDLER + "isolate", new IsolateNodeTypeHandler());
		
		ViNode node = cloud.node("test");
		ViProps.at(node).setIsolateType();
		Console.at(node).echoPrefix("[TEST] ");
		node.touch();
		String r = node.exec(new Callable<String>() {
			@Override
			public String call() {
				System.out.println("Hallo world!");
				System.out.println("My classloader is " + getClass().getClassLoader());
				return"ping";
			}
		});

		Assert.assertEquals("ping", r);
		
		cloud.shutdown();
	}
}
