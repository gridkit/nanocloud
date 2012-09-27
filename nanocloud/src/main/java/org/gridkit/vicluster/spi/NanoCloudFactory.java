package org.gridkit.vicluster.spi;

import org.gridkit.vicluster.NanoNode;
import org.gridkit.vicluster.ViCloud;
import org.gridkit.vicluster.telecontrol.spi.LocalControlledHost;

public class NanoCloudFactory {

	static CloudConfigSet defaultNanoCloudConfig() {
		
		RuleSet rules  = new RuleSet();
		rules.addRule(RuleBuilder.newRule()
			.condition()
				.type(ViNodeSpi.class)
			.defaultValue()
				.instantiator(new NanoNodeInstantiator())
		);

		rules.addRule(RuleBuilder.newRule()
			.condition()
				.type(ViNodeSpi.class)
			.defaultValue()
				.instantiator(new NanoNodeInstantiator())
		);

		rules.addRule(RuleBuilder.newRule()
			.condition()
				.type(ViNodeSpi.class)
			.defaultValue()
				.instantiator(NanoNodeInstantiator.TASK_SERVICE, new SharedTaskServiceInstantiator())
		);

		rules.addRule(RuleBuilder.newRule()
			.condition()
				.type(ViNodeSpi.class)
			.defaultValue()
				.instantiator(NanoNodeInstantiator.HOST, new UnconfiguredInstantiator())
		);

		rules.addRule(RuleBuilder.newRule()
			.condition()
				.type(Host.class)
			.defaultValue()
				.instantiator(new RemoteHostInstantiator())
		);		

		rules.addRule(RuleBuilder.newRule()
			.condition()
				.type(Host.class)
			.defaultValue()
				// use bean name as hostname by default
				.a(RemoteAttrs.HOST_HOSTNAME, new CopyAttr(AttrBag.NAME))
		);		

		rules.addRule(RuleBuilder.newRule()
			.condition()
				.type(HostConfiguration.class)
			.defaultValue()
				.instantiator(new HostConfigInstantiator())
		);		

		rules.addRule(RuleBuilder.newRule()
			.condition()
				.type(JvmProcessConfiguration.class)
			.defaultValue()
				.instantiator(new JvmConfigInstantiator())
		);	

		rules.addRule(RuleBuilder.newPrototype()
			.activation()
				.name("default")
				.type(LocalControlledHost.class)
			.configuration()
				.instantiator(new ReflectionInstantiator())
				.implementationClass(LocalControlledHost.class)
		);	
		
		return rules;
	}
	
	public static NanoCloudConfig newConfig() {
		return new NanoCloudConfig();
	}
	
	public static ViCloud<NanoNode> createCloud() {
		CloudManager<NanoNode> cloud = CloudManager.newIstance(NanoNode.class, new IsolateViNodeExtention());
		cloud.applyConfig(defaultNanoCloudConfig());		
		return cloud;				
	}	
}
