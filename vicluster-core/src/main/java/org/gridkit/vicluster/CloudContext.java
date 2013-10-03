package org.gridkit.vicluster;

import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;

public interface CloudContext {

	public <T> T lookup(ServiceKey<T> key);

	public <T> T lookup(ServiceKey<T> key, Callable<T> initializer);
	
	
	public static class ServiceKey<T> {
		
		private Class<T> type;
		private java.util.Map<String, String> props = new LinkedHashMap<String, String>();

		public ServiceKey(Class<T> type) {
			this.type = type;
		}

		public ServiceKey(Class<T> type, java.util.Map<String, String> keyProps) {
			this.type = type;
			props.putAll(keyProps);
		}
		
		public java.util.Map<String, String> asComparableMap() {
			return new TreeMap<String, String> (props);
		}
		
		public Class<T> getType() {
			return type;
		}
		
		public ServiceKey<T> with(String key, String value) {
			ServiceKey<T> that = new ServiceKey<T>(type, props);
			that.props.put(key, value);
			return that;
		}
		
		@Override
		public int hashCode() {
			return asComparableMap().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ServiceKey) {
				return asComparableMap().equals(((ServiceKey<?>) obj).asComparableMap());
			}
			else {
				return false;
			}
		}

		public String toString() {
			return type.getSimpleName() + props.toString();
		}
	}
}
