package org.gridkit.vicluster.spi;

import java.util.Arrays;
import java.util.List;

import org.gridkit.util.concurrent.SensibleTaskService;
import org.gridkit.vicluster.ViCloud2;
import org.gridkit.vicluster.isolate.IsolateViNode;

public class IsolateCloudFactory {

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
				.instantiator(new DefaultNodeInstantiator())
		);
		
		return rules;
	}
	
	public static ViCloud2<IsolateViNode> createIsolateCloud() {
		List<ViCloudExtention<?>> extentions = Any.cast(Arrays.asList(
				new IsolateViNodeExtention()
		));
		NanoCloud<IsolateViNode> cloud = new NanoCloud<IsolateViNode>(IsolateViNode.class, extentions, SensibleTaskService.getShareInstance());
		cloud.applyConfig(defaultIsolateRules());
		
		return cloud;		
	}	
}
