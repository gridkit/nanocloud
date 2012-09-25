package org.gridkit.vicluster.spi;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import junit.framework.Assert;

import org.gridkit.util.concurrent.SensibleTaskService;
import org.gridkit.vicluster.ViNode;
import org.junit.Before;
import org.junit.Test;

public class NanoClouldEnvTest implements ViSpiConsts {

	CloudManager<ViNode> ncloud;
	
	@Before
	public void init() {
		ncloud = new CloudManager<ViNode>(ViNode.class, null, new SensibleTaskService("test"));
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

		rules.addRule(RuleBuilder.newRule()
			.condition()
			    .matchName("**.mock_exec")
				.type(ViNodeSpi.class)
			.defaultValue()
				.a(EXECUTOR_PROVIDER, "mock")
		);

		rules.addRule(RuleBuilder.newRule()
			.condition()
				.label("mock")
				.type(ViNodeSpi.class)
			.defaultValue()
				.a(EXECUTOR_PROVIDER, "mock")
		);

		rules.addRule(RuleBuilder.newPrototype()
			.activation()
				.name("default")
				.type(ExecutorProvider.class)
			.configuration()
				.implementationClass(ThreadPoolExecutorProvider.class)
		);

		rules.addRule(RuleBuilder.newPrototype()
			.activation()
				.name("mock")
				.type(ExecutorProvider.class)
			.configuration()
				.implementationClass(MockExecutorProvider.class)
				.a("mock", "XXX")
		);
		
		ncloud.applyConfig(rules);
	}
	
	
	@Test
	public void test_something() throws InterruptedException, ExecutionException {
		Future<String> a = ncloud.byName("node.a").submit(new Echo("A"));
		
		Assert.assertEquals("A", a.get());
		Assert.assertEquals(Arrays.asList("A"), ncloud.byName("node.a").vectorSubmit(new Echo("A")).get());
	}

	@Test
	public void test_something2() throws InterruptedException, ExecutionException {
		ncloud.byName("node.a");
		ncloud.byName("node.b");
		
		Assert.assertEquals(Arrays.asList("A", "A"), ncloud.byName("node.*").vectorSubmit(new Echo("A")).get());
	}

	@Test
	public void test_mock_executor() throws InterruptedException, ExecutionException {
		ViNode node = ncloud.byName("node.mock_exec");
		System.out.println(node.toString());
		
		Assert.assertEquals("XXX", node.exec(new Echo("A")));
	}

	@Test
	public void test_mock_executor2() throws InterruptedException, ExecutionException {
		ViNode node = ncloud.byName("node.a");
		node.labels().add("mock");
		System.out.println(node.toString());
		
		Assert.assertEquals("XXX", node.exec(new Echo("A")));
	}

	@Test
	public void test_mock_executor3() throws InterruptedException, ExecutionException {
		ncloud.byName("node.a1");
		ncloud.byName("node.b");
		ncloud.byName("node.a*").labels().add("mock");
		ncloud.byName("node.a2");

		System.out.println(ncloud.byName("node.*").toString());
		
		Assert.assertEquals(Arrays.asList(
				"XXX",
				"XXX",
				"A"), ncloud.byName("node.*").massExec(new Echo("A")));
	}

	@Test
	public void test_mock_executor4() throws InterruptedException, ExecutionException {
		ncloud.byName("node.a1").label("mock");
		ncloud.byName("node.b");
		ncloud.byName("node.a2");
		
		System.out.println(ncloud.byName("node.*").toString());
		
		Assert.assertEquals(Arrays.asList(
				"XXX",
				"A",
				"A"), ncloud.byName("node.*").massExec(new Echo("A")));
	}

	@Test
	public void test_user_props() throws InterruptedException, ExecutionException {
		ncloud.byName("node.a1").userProps().put("A", "A");
		ncloud.byName("node.a2").userProps().put("B", "B");
		
		Assert.assertEquals("A", ncloud.byName("node.a1").userProps().get("A"));
		Assert.assertEquals("B", ncloud.byName("node.a2").userProps().get("B"));
	}

	@Test
	public void test_user_props2() throws InterruptedException, ExecutionException {
		ncloud.byName("node.a1");
		ncloud.byName("node.a2");
		ncloud.byName("node.*").userProps().put("A", "B");
		
		Assert.assertEquals("B", ncloud.byName("node.a1").userProps().get("A"));
		Assert.assertEquals("B", ncloud.byName("node.a2").userProps().get("A"));
	}

	@Test
	public void test_user_props3() throws InterruptedException, ExecutionException {
		ncloud.byName("node.*").userProps().put("A", "B");
		ncloud.byName("node.a1");
		ncloud.byName("node.a2");
		
		Assert.assertEquals("B", ncloud.byName("node.a1").userProps().get("A"));
		Assert.assertEquals("B", ncloud.byName("node.a2").userProps().get("A"));
	}

	@SuppressWarnings("serial")
	private class Echo implements Callable<String>, Serializable {
		
		private final String value;
		
		public Echo(String value) {
			this.value = value;
		}

		@Override
		public String call() throws Exception {
			return value;
		}
	}	
}
