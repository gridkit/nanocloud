package org.gridkit.vicluster.spi;

import java.util.ArrayList;
import java.util.List;

class RuleSet implements CloudConfigSet {
	
	private List<RuleBuilder.Rule> rules = new ArrayList<RuleBuilder.Rule>();
	
	public void addRule(RuleBuilder.Rule rule) {
		rules.add(rule);
	}
	
	public void apply(CloudContext context) {
		for(RuleBuilder.Rule rule: rules) {
			rule.apply(context);
		}
	}
}
