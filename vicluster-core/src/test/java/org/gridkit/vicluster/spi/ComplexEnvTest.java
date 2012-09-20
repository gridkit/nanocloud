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
		{
			// default provider rule
			Selector s1 = Selectors.isNotSet(NODE_PROVIDER);
			Selector s2 = Selectors.is(TYPE, NODE_TYPE);
			Selector s3 = Selectors.allOf(s1, s2);
			
			Set action = new ConfigRuleAction.Set(NODE_PROVIDER, "default");
			
			cloud.addRule(new ConfigRule(s3, action));
		}
		{
			Selector s1 = Selectors.isNotSet(PROVIDER);

			Set action = new ConfigRuleAction.Set(PROVIDER, new ReflectionInstantiator());

			cloud.addRule(new ConfigRule(s1, action));
		}
		{
			Selector s1 = Selectors.isNotSet(IMPL_CLASS);
			
			Set action = new ConfigRuleAction.Set(IMPL_CLASS, TestNodeProvider.class.getName());
			
			cloud.addRule(new ConfigRule(s1, action));
		}
	}
	
	
	@Test
	public void verify_configuration() {
		
		AttrList proto = new AttrList();
		proto.add(TYPE, NODE_TYPE);
		proto.add(ID, "node.x");
		
		AttrBag bean = cloud.ensureResource(proto);
		
		Assert.assertEquals("default", bean.getLast(PROVIDER));
		
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
