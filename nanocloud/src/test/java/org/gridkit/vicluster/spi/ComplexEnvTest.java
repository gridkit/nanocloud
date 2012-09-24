package org.gridkit.vicluster.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.gridkit.vicluster.spi.ConfigRuleAction.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ComplexEnvTest implements ViSpiConsts {

	CloudContext cloud;
	
	@Before
	public void initContext() {
		cloud = new CloudContext();
		RuleBuilder.startRules()
		.rule()
			.condition()
			.defaultValue()
				.instantiator(new ReflectionInstantiator())
			.apply(cloud)
		
		.rule()
			.condition()
				.type(ViNodeSpi.class)
			.defaultValue()
				.instantiator(new DefaultNodeInstantiator())
			.apply(cloud)
		
		.rule()
			.condition()
				.type(ViNodeSpi.class)
			.defaultValue()
				.a(EXECUTOR_PROVIDER, "default")
			.apply(cloud)
			
		.prototype()
			.activation()
				.name("default")
				.type(ExecutorProvider.class)
			.configuration()
				.implementationClass(ThreadPoolExecutorProvider.class)
			.apply(cloud);				
	}
	

	private void setDefaultInstantiator() {
		Selector s1 = Selectors.isNotSet(INSTANCE);
		Set action = new ConfigRuleAction.Set(INSTANCE, new ReflectionInstantiator());
		cloud.addRule(new ConfigRule(s1, action));
	}

	private void setDefaultViNodeInstantiator() {
		// default provider rule
		Selector s1 = Selectors.isNotSet(INSTANCE);
		Selector s2 = Selectors.is(TYPE, NODE_TYPE);
		Selector s3 = Selectors.allOf(s1, s2);
		Set action = new ConfigRuleAction.Set(INSTANCE, new DefaultNodeInstantiator());
		cloud.addRule(new ConfigRule(s3, action));
	}

	private void setDefaultExecutorForNodes() {
		// default provider rule
		Selector s1 = Selectors.isNotSet(EXECUTOR_PROVIDER);
		Selector s2 = Selectors.is(TYPE, NODE_TYPE);
		Selector s3 = Selectors.allOf(s1, s2);
		Set action = new ConfigRuleAction.Set(EXECUTOR_PROVIDER, "default");
		cloud.addRule(new ConfigRule(s3, action));
	}

	private void setDefaultExecutorProvider() {
		Selector s1 = Selectors.isNotSet(INSTANCE);
		Selector s2 = Selectors.is(TYPE, EXECUTOR_PROVIDER_TYPE);
		Selector s3 = Selectors.allOf(s1, s2);
		Set action = new Set(IMPL_CLASS, ThreadPoolExecutorProvider.class.getName());		
		cloud.addRule(new ConfigRule(s3, action));
	}

	@Test
	public void verify_configuration() {
		
		AttrList proto = new AttrList();
		proto.add(TYPE, NODE_TYPE);
		proto.add(NAME, "node.x");
		
		AttrBag bean = cloud.ensureResource(proto);

		ViNodeSpi x = cloud.getNamedInstance("node.x", ViNodeSpi.class);
		
		
		
	}
	
	static ConfigRule setDefaultRule(Selector s, AttrList attrList) {
		List<Selector> condition = new ArrayList<Selector>();
		List<ConfigRuleAction> actions = new ArrayList<ConfigRuleAction>();
		condition.add(s);
		for(Entry<String, Object> entry: attrList) {
			condition.add(Selectors.is(entry.getKey(), entry.getValue()));
			actions.add(new Set(entry.getKey(), entry.getValue()));
		}
		return new ConfigRule(Selectors.allOf(condition), actions);
	}
}
