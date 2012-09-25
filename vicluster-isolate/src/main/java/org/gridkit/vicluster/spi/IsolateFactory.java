package org.gridkit.vicluster.spi;

import org.gridkit.vicluster.ViCloud;
import org.gridkit.vicluster.isolate.IsolateViNode;

public class IsolateFactory {

	/**
	 * @return Very simple rule set capable of running isolate only cloud
	 */
	public static CloudConfigSet defaultIsolateRules() {
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
				.instantiator(new IsolateNodeInstantiator())
		);

		rules.addRule(RuleBuilder.newRule()
			.condition()
				.type(ViNodeSpi.class)
			.defaultValue()
				.a(IsolateInstantiator.ISOLATE, new IsolateInstantiator())
		);
		
		return rules;
	}
	
	public static ViCloud<IsolateViNode> createIsolateCloud() {
		CloudManager<IsolateViNode> cloud = CloudManager.newIstance(IsolateViNode.class, new IsolateViNodeExtention());
		cloud.applyConfig(defaultIsolateRules());		
		return cloud;		
	}	
}
