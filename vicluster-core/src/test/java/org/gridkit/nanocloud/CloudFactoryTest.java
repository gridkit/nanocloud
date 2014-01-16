package org.gridkit.nanocloud;

import java.io.StringWriter;
import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.VX;
import org.junit.After;
import org.junit.Test;

public class CloudFactoryTest {

	public Cloud cloud = initCloud();
	
	public Cloud initCloud() {
		Cloud cloud = CloudFactory.createCloud();
		ViProps.at(cloud.node("**")).setLocalType();
		return cloud;
	}
	
	@After
	public void dropCloud() {
		cloud.shutdown();
	}
	
	@Test
	public void ping_local_node() {
		ViNode node = cloud.node("test");
		node.touch();
		String r = node.exec(new Callable<String>() {
			@Override
			public String call() {
				System.out.println("Hallo world!");
				return"ping";
			}
		});

		Assert.assertEquals("ping", r);
	}
	
	@Test
	public void capture_console_local_node() {
		ViNode node = cloud.node("test");
		node.touch();
		
		StringWriter outwriter = new StringWriter();
		StringWriter errwriter = new StringWriter();
		node.x(VX.CONSOLE).bindOut(outwriter);
		node.x(VX.CONSOLE).bindOut(outwriter);
		node.x(VX.CONSOLE).bindErr(errwriter);
		
		String r = node.exec(new Callable<String>() {
			@Override
			public String call() {
				System.out.println("ping");
				System.err.println("pong");
				return"ping";
			}
		});
		
		node.x(VX.CONSOLE).flush();

		Assert.assertEquals("ping", r);
		Assert.assertTrue(outwriter.toString().startsWith("ping"));
		Assert.assertTrue(errwriter.toString().startsWith("pong"));

		node.x(VX.CONSOLE).echoPrefix("~[%s-xx] !(.*)");

		node.exec(new Callable<Void>() {
			@Override
			public Void call() {
				System.out.println("This line should use diffent prefix");
				return null;
			}
		});
	}	
}
