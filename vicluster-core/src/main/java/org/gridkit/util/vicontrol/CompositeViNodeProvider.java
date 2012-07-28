package org.gridkit.util.vicontrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CompositeViNodeProvider implements ViNodeProvider {

	private List<Provider> providers = new ArrayList<Provider>();
	
	public void addProvider(Map<String, String> selector, ViNodeProvider provider) {
		providers.add(new Provider(selector, provider));
	}
	
	@Override
	public boolean verifyNodeConfig(ViNodeConfig config) {
		// TODO
		return true;
	}

	@Override
	public ViNode createNode(String name, ViNodeConfig config) {
		Provider p = lookup(config);
		if (p == null) {
			throw new IllegalArgumentException("Cannot find provider for node '" + name + "'");
		}
		return p.provider.createNode(name, config);
	}

	private Provider lookup(ViNodeConfig config) {
		for(Provider provider: providers) {
			if (provider.match(config)) {
				return provider;
			}
		}
		return null;
	}

	private static class Provider {
		
		private Map<String, Object> selector;
		private ViNodeProvider provider;
		
		public Provider(Map<String, String> selector, ViNodeProvider provider) {
			this.selector = translate(selector);
			this.provider = provider;
		}

		private Map<String, Object> translate(Map<String, String> selector) {
			Map<String, Object> t = new HashMap<String, Object>();
			for(String key: selector.keySet()) {
				String value = selector.get(key);
				// TODO robust notation for matchers 
				if (value.startsWith("glob:")) {
					String pattern = value.substring("glob:".length());
					t.put(key, GlobHelper.translate(pattern, "."));
				}
				else {
					t.put(key, value);
				}
			}
			return t;
		}
		
		public boolean match(ViNodeConfig config) {
			for(String key: selector.keySet()) {
				Object matcher = selector.get(key);
				String value = config.getProp(key);
				value = value == null ? "" : value;
				if (!match(matcher, value)) {
					return false;
				}
			}
			return true;
		}

		private boolean match(Object matcher, String value) {
			if (matcher instanceof String) {
				return matcher.equals(value);
			}
			else {
				return ((Pattern)matcher).matcher(value).matches();
			}
		}
	}
}
