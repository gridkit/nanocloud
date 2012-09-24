package org.gridkit.vicluster.spi;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import junit.framework.Assert;

import org.gridkit.util.concurrent.SensibleTaskService;
import org.gridkit.vicluster.ViNode;
import org.junit.Before;
import org.junit.Test;

public class NanoClouldEnvTest implements ViSpiConsts {

	NanoCloud<ViNode> ncloud;
	
	@Before
	public void init() {
		ncloud = new NanoCloud<ViNode>(ViNode.class, null, new SensibleTaskService("test"));
		RuleSet rules = new RuleSet();

		rules.addRule(RuleBuilder.newRule()
			.condition()
			.defaultValue()
				.instantiator(new ReflectionInstantiator())
		);
		
		rules.addRule(RuleBuilder.newRule()
			.condition()
				.type(ViNodeSpi.class)
			.defaultValue()
				.instantiator(new DefaultNodeInstantiator())
		);
		
		rules.addRule(RuleBuilder.newRule()
			.condition()
				.type(ViNodeSpi.class)
			.defaultValue()
				.a(EXECUTOR_PROVIDER, "default")
		);
			
		rules.addRule(RuleBuilder.newPrototype()
			.activation()
				.name("default")
				.type(ExecutorProvider.class)
			.configuration()
				.implementationClass(ThreadPoolExecutorProvider.class)
		);
		
		ncloud.applyConfig(rules);
	}
	
	
	@Test
	public void test_something() throws InterruptedException, ExecutionException {
		Future<String> a = ncloud.byName("node.a").submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "A";
			}
		});
		
		Assert.assertEquals("A", a.get());
	}
	
}
