package org.gridkit.vicluster.spi;

import java.util.Arrays;
import java.util.List;

class ConfigRule implements Selector, ConfigRuleAction {
	
	private final Selector selector;
	private final List<ConfigRuleAction> actions;
	
	public ConfigRule(Selector selector, ConfigRuleAction... actions) {
		this(selector, Arrays.asList(actions));
	}

	public ConfigRule(Selector selector, List<ConfigRuleAction> actions) {
		this.selector = selector;
		this.actions = actions;
	}

	@Override
	public boolean match(AttrBag bag) {
		return selector.match(bag);
	}

	@Override
	public void fire(BeanConfig config) {
		for(ConfigRuleAction action: actions) {
			action.fire(config);
		}		
	}
}
