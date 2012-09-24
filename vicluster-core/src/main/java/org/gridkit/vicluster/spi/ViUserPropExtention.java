package org.gridkit.vicluster.spi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.vicluster.ViUserPropEnabled;

class ViUserPropExtention implements ViCloudExtention<ViUserPropEnabled> {

	private static final Method PROXY_PUT;
	private static final Method PROXY_PUT_ALL; 
	private static final Method PROXY_REMOVE;
	static {
		try {
			PROXY_PUT = PropWriteProxy.class.getMethod("put", String.class, Object.class);
			PROXY_PUT.setAccessible(true);
			PROXY_PUT_ALL = PropWriteProxy.class.getMethod("putAll", Map.class);
			PROXY_PUT_ALL.setAccessible(true);
			PROXY_REMOVE = PropWriteProxy.class.getMethod("remove", String.class);
			PROXY_REMOVE.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	
	@Override
	public Class<ViUserPropEnabled> getFacadeInterface() {
		return ViUserPropEnabled.class;
	}

	@Override
	public Class<?>[] getHidenInterfaces() {
		return new Class<?>[]{PropWriteProxy.class};
	}

	@Override
	public DeferingMode deferingModeForMethod(Method m) {
		return DeferingMode.NO_SPI_NEEDED;
	}

	@Override
	public GroupCallMode groupModeForMethod(Method m) {
		if (m.getName().equals("userProps")) {
			return GroupCallMode.BY_IMPLEMENTATION;
		}
		else {
			return GroupCallMode.STICKY_BROADCAST;
		}
	}

	@Override
	public void processNodeConfig(DynNode node, AttrList config) {
		// do nothing
	}

	@Override
	public ViUserPropEnabled wrapSingle(DynNode node) {
		return new PropHost();
	}

	@Override
	public ViUserPropEnabled wrapMultiple(NodeCallProxy group, DynNode[] nodes) {
		return new PropGroupProxy(group);
	}

	public static class PropHost implements ViUserPropEnabled, PropWriteProxy {

		private Map<String, Object> props = new HashMap<String, Object>();
		
		@Override
		public Map<String, Object> userProps() {
			return props;
		}

		@Override
		public void remove(String key) {
			props.remove(key);
		}

		@Override
		public void put(String key, Object value) {
			props.put(key, value);
		}

		@Override
		public void putAll(Map<String, Object> props) {
			this.props.putAll(props);
		}
	}
	
	public static class PropGroupProxy implements ViUserPropEnabled {

		private final NodeCallProxy groupProxy;
		
		public PropGroupProxy(NodeCallProxy groupProxy) {
			this.groupProxy = groupProxy;
		}

		@Override
		public Map<String, Object> userProps() {
			return new WriteThroughMap<String, Object>(new HashMap<String, Object>()) {

				@Override
				protected boolean onPut(String key, Object value) {
					try {
						groupProxy.dispatch(PROXY_PUT, key, value);
					} catch (Throwable e) {
						if (e instanceof InvocationTargetException) {
							e = e.getCause();
						}
						Any.throwUncheked(e);
						throw new Error("Unreachable");
					}
					return false;
				}
				
				@Override
				public void putAll(Map<? extends String, ? extends Object> m) {
					try {
						groupProxy.dispatch(PROXY_PUT_ALL, m);
					} catch (Throwable e) {
						if (e instanceof InvocationTargetException) {
							e = e.getCause();
						}
						Any.throwUncheked(e);
					}
				}

				@Override
				protected boolean onRemove(String key) {
					try {
						groupProxy.dispatch(PROXY_REMOVE, key);
					} catch (Throwable e) {
						if (e instanceof InvocationTargetException) {
							e = e.getCause();
						}
						Any.throwUncheked(e);
						throw new Error("Unreachable");
					}
					return false;
				}
			};
		}
	}
	
	public static interface PropWriteProxy {
		
		public void put(String key, Object value);

		public void remove(String key);
		
		public void putAll(Map<String, Object> props);
	}
}
