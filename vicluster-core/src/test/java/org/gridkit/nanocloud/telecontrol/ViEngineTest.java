package org.gridkit.nanocloud.telecontrol;

import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.gridkit.vicluster.AbstractCloudContext;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.telecontrol.jvm.ViEngineNodeProvider;
import org.junit.After;
import org.junit.Test;

public class ViEngineTest {

	private CloudContext ctx = new CloudContext();
	
	@After
	public void dropCloud() {
		ctx.runFinalizers();
	}
	
//	@Test
//	public void verify_default_local_node_start_up() throws InterruptedException {
//		Map<String, Object> config = new HashMap<String, Object>();
//		config.put(ViConf.NODE_NAME, "test-node");
//		config.put(ViConf.SPI_CLOUD_CONTEXT, ctx);
//		config.put(ViConf.NODE_TYPE, "local");
//		config.put(ViConf.TYPE_HANDLER + "local", new LocalNodeTypeHandler());
//		
//		config.put(ViConf.HOOK_NODE_INITIALIZER, new ViEngine.DefaultInitRuleSet());
//		
//		ViEngine.Core veng = new ViEngine.Core();
//		config = veng.processPhase(Phase.PRE_INIT, config);
//		
//		ViNode node = (ViNode) config.get(ViConf.SPI_NODE_INSTANCE);
//		Assert.assertNotNull(node);
//		
//		String ping = node.exec(new Callable<String>() {
//			@Override
//			public String call() throws Exception {
//				System.out.println("Ping");
//				return "ping";
//			}
//		});
//		
//		Assert.assertEquals("ping", ping);
//		
//		node.shutdown();
//	}

	@Test
	public void vi_engine_node_test() throws InterruptedException {
		ViManager cloud = new ViManager(new ViEngineNodeProvider());
		
		cloud.node("**").setConfigElement(ViConf.TYPE_HANDLER + "local", new LocalNodeTypeHandler());
		
		ViNode node = cloud.node("test");
		ViProps.at(node).setLocalType();
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
