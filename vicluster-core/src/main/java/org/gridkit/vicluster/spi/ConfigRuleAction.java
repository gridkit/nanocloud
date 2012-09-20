package org.gridkit.vicluster.spi;

public interface ConfigRuleAction {

	public void fire(BeanConfig config);
	
	public static class Set implements ConfigRuleAction {
		
		private String name;
		private Object value;
		
		public Set(String name, Object value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public void fire(BeanConfig config) {
			config.addEntry(name, value);
		}
	}
}
