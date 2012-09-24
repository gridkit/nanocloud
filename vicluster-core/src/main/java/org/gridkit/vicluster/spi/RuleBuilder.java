package org.gridkit.vicluster.spi;

import java.util.ArrayList;
import java.util.List;

import org.gridkit.vicluster.spi.RuleBuilder.GenericRuleS2;

public class RuleBuilder {
	
	public static StartRules startRules() {
		return new StartRules() {
			
			@Override
			public GenericRuleS1 rule() {
				return newRule();
			}
			
			@Override
			public BeanRuleS1 prototype() {
				return newPrototype();
			}
		};
	}
	
	public static BeanRuleS1 newPrototype() {
		return new RuleBuilderImpl().beanRuleS1();
	}

	public static GenericRuleS1 newRule() {
		return new RuleBuilderImpl().genericRuleS1();
	}
	
	public interface Rule {
		StartRules apply(CloudContext context);
	}
	
	public interface StartRules {
		
		public BeanRuleS1 prototype();
		
		public GenericRuleS1 rule();
		
	}
	
	public interface BeanRuleS1 {		
		
		BeanRuleS2 activation();
		
	}
	
	public interface BeanRuleS2 {

		BeanRuleS2 a(String attr, Object value);		
		BeanRuleS2 name(String name);
		BeanRuleS2 type(String type);
		BeanRuleS2 type(Class<?> type);
		BeanRule configuration();
		
	}
	
	public interface BeanRule extends Rule {

		BeanRule a(String attr, Object value);

		BeanRule implementationClass(Class<?> type);		
	}
	
	public interface GenericRuleS1 {		
		GenericRuleS2 condition();		
	}

	public interface GenericRuleS2 {		
		GenericRuleS2 matchName(String name);
		GenericRuleS2 name(String name);
		GenericRuleS2 label(String label);
		GenericRuleS2 type(String type);		
		GenericRuleS2 type(Class<?> type);		
		DefaultRuleS1 defaultValue();		
		GenericRule configuration();		
	}

	public interface DefaultRuleS1 {		
		
		Rule a(String name, Object value);
		Rule instantiator(SpiFactory factory);				
	}

	public interface GenericRule extends Rule {		
		GenericRule a(String name, Object value);		
	}

	private static class RuleBuilderImpl {
		
		private List<Selector> conditions = new ArrayList<Selector>();
		private List<ConfigRuleAction> actions = new ArrayList<ConfigRuleAction>();
		private boolean completed;
		
		public void doApply(CloudContext context) {
			if (!completed) {
				throw new IllegalStateException("Rule is not complete");
			}
			ConfigRule rule = new ConfigRule(Selectors.allOf(conditions), actions);
			context.addRule(rule);			
		}
		
		void addToHead(String attr, Object value) {
			conditions.add(Selectors.is(attr, value));
		}
		
		void addToConf(String attr, Object value) {
			actions.add(new ConfigRuleAction.Set(attr, value));
		}

		public BeanRuleS1 beanRuleS1() {
			return new BeanRuleS1() {				
				@Override
				public BeanRuleS2 activation() {
					return beanRulesS2();
				}
			};
		}

		public BeanRuleS2 beanRulesS2() {
			return new BeanRuleS2() {

				@Override
				public BeanRuleS2 a(String attr, Object value) {
					addToHead(attr, value);
					addToConf(attr, value);
					return this;
				}

				@Override
				public BeanRuleS2 name(String name) {
					a(AttrBag.NAME, name);
					return this;
				}

				@Override
				public BeanRuleS2 type(String type) {
					a(AttrBag.TYPE, type);
					return this;
				}
				
				@Override
				public BeanRuleS2 type(Class<?> type) {
					type(type.getName());
					return this;
				}

				@Override
				public BeanRule configuration() {
					return beanRuleS3();
				}
			};
		}

		public BeanRule beanRuleS3() {
			completed = true;
			return new BeanRule() {

				@Override
				public BeanRule a(String attr, Object value) {
					addToConf(attr, value);
					return this;
				}

				@Override
				public BeanRule implementationClass(Class<?> type) {
					a(ViSpiConsts.IMPL_CLASS, type.getName());
					return this;
				}

				@Override
				public StartRules apply(CloudContext context) {
					doApply(context);
					return startRules();
				}				
			};
		}
		
		public GenericRuleS1 genericRuleS1() {
			return new GenericRuleS1() {
				@Override
				public GenericRuleS2 condition() {
					return genericRuleS2();
				}
			};
		}

		public GenericRuleS2 genericRuleS2() {
			return new GenericRuleS2() {

				@Override
				public GenericRuleS2 matchName(String name) {
					conditions.add(Selectors.match(AttrBag.NAME, GlobHelper.translate(name, ".").pattern()));
					return this;
				}

				@Override
				public GenericRuleS2 name(String name) {
					conditions.add(Selectors.is(AttrBag.NAME, name));
					return this;
				}

				@Override
				public GenericRuleS2 label(String label) {
					conditions.add(Selectors.has(AttrBag.LABEL, label));
					return this;
				}

				@Override
				public GenericRuleS2 type(String type) {
					addToHead(AttrBag.TYPE, type);
					return this;
				}

				@Override
				public GenericRuleS2 type(Class<?> type) {
					type(type.getName());
					return this;
				}

				@Override
				public DefaultRuleS1 defaultValue() {
					return defaultRuleS1();
				}

				@Override
				public GenericRule configuration() {
					return genericRuleS3();
				}				
			};
		}

		public GenericRule genericRuleS3() {
			completed = true;
			return new GenericRule() {

				@Override
				public GenericRule a(String name, Object value) {
					addToConf(name, value);
					return null;
				}

				@Override
				public StartRules apply(CloudContext context) {
					doApply(context);
					return startRules();
				}
			};
		}

		public DefaultRuleS1 defaultRuleS1() {
			return new DefaultRuleS1() {
				@Override
				public Rule a(String name, Object value) {
					conditions.add(Selectors.isNotSet(name));
					addToConf(name, value);
					return genericRuleS3();
				}

				@Override
				public Rule instantiator(SpiFactory instantiator) {					
					return a(AttrBag.INSTANCE, instantiator);
				}
			};
		}
	}	
}
