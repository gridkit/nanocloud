/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.vicluster;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gridkit.nanocloud.ViConfExtender;
import org.gridkit.nanocloud.ViConfigurable;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@SuppressWarnings("serial")
public class ViNodeConfig extends ViConf implements ViConfigurable, Serializable {

	public ViNodeConfig() {
		super(new LinkedHashMap<String, Object>());
	}

	@Override
	public <X> X x(ViConfExtender<X> extention) {
		return extention.wrap(this);
	}	
	
	public String getProp(String propName) {
		return (String)config.get(propName);
	}

	public String getProp(String propName, String def) {
		String val = (String)(config.get(propName));
		return val == null ? def : val;
	}

	public Map<String, String> getAllProps(String prefix) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		for(String key: config.keySet()) {
			if (key.startsWith(prefix)) {
				result.put(key, (String)config.get(key));
			}			
		}
		return result;
	}

	/**
	 * @return all non-special (not containing ':' in name props)
	 */
	public Map<String, String> getAllVanilaProps() {
		Map<String, String> result = new LinkedHashMap<String, String>();
		for(String key: config.keySet()) {
			if (key.indexOf(':') < 0) {
				result.put(key, (String)config.get(key));
			}			
		}
		return result;
	}
	
	public Map<String, Object> getInternalConfigMap() {
		return Collections.unmodifiableMap(config);
	}
	
	@Override
	public void setProp(String propName, String value) {
		setConfigElement(propName, value);
	}

	@Override
	public void setProps(Map<String, String> props) {
		for(String key: props.keySet()) {
			setConfigElement(key, props.get(key));
		}
	}

	@Override
	public void setConfigElement(String key, Object value) {
		if (key.indexOf(':') < 0) {
			// vanila properties should be strings
			String pv = (String) value;
			config.put(key, pv);
		}
		else {
			// TODO opportunistic type validation
			config.put(key, value);
		}
	}

	@Override
	public void setConfigElements(Map<String, Object> config) {
		for(String key: config.keySet()) {
			setConfigElement(key, config.get(key));
		}
	}
	
	public void apply(ViConfigurable target) {
		target.setConfigElements(config);
	}	
	
	public static void applyProps(ViConfigurable vc, Map<String, String> props) {
		for(Map.Entry<String, String> e : props.entrySet()) {
			vc.setProp(e.getKey(), e.getValue());
		}
	}
	
	public static boolean matches(String prefix, String propName) {
		if (prefix.endsWith(":")) {
			return propName.startsWith(prefix);
		}
		else {
			return propName.equals(prefix);
		}
	}
	
	public static abstract class ConfigurationDispatcher implements ViConfigurable {

		@Override
		public void setConfigElement(String key, Object value) {
			// TODO full refactoring of configuration mechanics
			if (ViConf.isVanilaProp(key)) {
				setProp(key, (String)value);
			}
			else if (ViConf.isHook(key)) {
				// ignore
			}
			else {
				setProp(key, (String)value);
			}
		}

		@Override
		public void setConfigElements(Map<String, Object> config) {
			for(String key: config.keySet()) {
				setConfigElement(key, config.get(key));
			}
		}
	}
	
	public static abstract class ReplyVanilaProps extends ReplyProps {

		public ReplyVanilaProps(String... filter) {
			super(filter);
		}

		@Override
		public void setConfigElement(String key, Object value) {
			if (ViConf.isVanilaProp(key)) {
				super.setConfigElement(key, value);
			}
		}
	}

	public static abstract class ReplyProps extends ConfigurationDispatcher implements ViConfigurable {
		
		private String[] filter;

		public ReplyProps(String... filter) {
			this.filter = filter;
		}
		
		@Override
		public <X> X x(ViConfExtender<X> extention) {
			return extention.wrap(this);
		}
		
		protected abstract void setPropInternal(String propName, String value);
		
		@Override
		public void setProp(String propName, String value) {
			if (filter.length == 0) {
				setPropInternal(propName, value);
			}
			else {
				for(String f: filter) {
					if (matches(f, propName)) {
						setPropInternal(propName, value);
						break;
					}
				}
			}
		}
		
		@Override
		public void setProps(Map<String, String> props) {
			for(String key: props.keySet()) {
				setProp(key, props.get(key));
			}
		}
	}

	public static abstract class ReplyShutdownHooks implements ViConfigurable {
		
		@Override
		public <X> X x(ViConfExtender<X> extention) {
			return extention.wrap(this);
		}
		
		@Override
		public void setProp(String propName, String value) {
			// ignore
		}
		
		@Override
		public void setProps(Map<String, String> props) {
			// ignore
		}

		@Override
		public void setConfigElement(String key, Object value) {
			// ignore
		}

		@Override
		public void setConfigElements(Map<String, Object> config) {
			// ignore
		}
	}
}
