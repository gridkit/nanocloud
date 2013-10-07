package org.gridkit.nanocloud.telecontrol;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.gridkit.vicluster.AbstractCloudContext;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine;
import org.gridkit.vicluster.ViEngine.Phase;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.telecontrol.LocalNodeTypeHandler;
import org.junit.After;
import org.junit.Test;

public class ViEngineTest {

	private CloudContext ctx = new CloudContext();
	
	@After
	public void dropCloud() {
		ctx.runFinalizers();
	}
	
	@Test
	public void verify_default_local_node_start_up() {
		Map<String, Object> config = new HashMap<String, Object>();
		config.put(ViConf.SPI_CLOUD_CONTEXT, ctx);
		config.put(ViConf.NODE_TYPE, "local");
		config.put(ViConf.TYPE_HANDLER + "local", new LocalNodeTypeHandler());

		config.put(ViConf.HOOK_NODE_INITIALIZER, new ViEngine.DefaultInitRuleSet());
		
		ViEngine veng = new ViEngine();
		config = veng.processPhase(Phase.PRE_INIT, config);
		
		ViNode node = (ViNode) config.get(ViConf.SPI_NODE_INSTANCE);
		Assert.assertNotNull(node);
		
		String ping = node.exec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "ping";
			}
		});
		
		Assert.assertEquals("ping", ping);
		
		node.shutdown();
	}
	
	public static class CloudContext extends AbstractCloudContext {

		@Override
		protected synchronized void runFinalizers() {
			super.runFinalizers();
		}
	}
}
