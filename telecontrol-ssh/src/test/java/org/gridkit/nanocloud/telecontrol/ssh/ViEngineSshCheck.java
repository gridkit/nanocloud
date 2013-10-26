package org.gridkit.nanocloud.telecontrol.ssh;

import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.gridkit.vicluster.AbstractCloudContext;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.telecontrol.jvm.ViEngineNodeProvider;
import org.gridkit.vicluster.telecontrol.ssh.RemoteNodeProps;
import org.junit.After;
import org.junit.Test;

public class ViEngineSshCheck {

	private CloudContext ctx = new CloudContext();
	
	@After
	public void dropCloud() {
		ctx.runFinalizers();
	}
	
	@Test
	public void vi_engine_node_test() throws InterruptedException {
		ViManager cloud = new ViManager(new ViEngineNodeProvider());
		
		cloud.node("**").setConfigElement(ViConf.TYPE_HANDLER + "remote", new RemoteNodeTypeHandler());
		
		ViNode node = cloud.node("test");
		ViProps.at(node).setRemoteType();
		RemoteNodeProps.at(node).setRemoteHost("cbox1");
		node.setProp(ViConf.REMOTE_HOST_CONFIG,"?~/ssh-credentials.prop");
		node.setProp(SshSpiConf.SPI_BOOTSTRAP_JVM_EXEC, "java");
		node.setProp(SshSpiConf.SPI_JAR_CACHE, "/tmp/nanocloud");
		node.touch();
		String r = node.exec(new Callable<String>() {
			@Override
			public String call() {
				System.out.println("Hallo world!");
				return"ping";
			}
		});

		Assert.assertEquals("ping", r);
		
		cloud.shutdown();
	}
	
	public static class CloudContext extends AbstractCloudContext {

		@Override
		protected synchronized void runFinalizers() {
			super.runFinalizers();
		}
	}
}
