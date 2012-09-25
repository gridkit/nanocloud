package org.gridkit.vicluster.spi;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.gridkit.vicluster.SysProps;
import org.gridkit.vicluster.ViSysPropEnabled;
import org.gridkit.vicluster.spi.NodeSpiHelper.MethodMode;

class ViSysPropExtention implements ViCloudExtention<ViSysPropEnabled> {

	@Override
	public Class<ViSysPropEnabled> getFacadeInterface() {
		return ViSysPropEnabled.class;
	}

	@Override
	public Class<?>[] getHidenInterfaces() {
		return new Class<?>[]{SysProps.class};
	}

	@Override
	public DeferingMode deferingModeForMethod(Method m) {
		return NodeSpiHelper.getMethodModeAnnotation(NodeWrapper.class, m).deferNode();
	}

	@Override
	public GroupCallMode groupModeForMethod(Method m) {
		return NodeSpiHelper.getMethodModeAnnotation(NodeWrapper.class, m).groupCallNode();
	}

	@Override
	public void processNodeConfig(DynNode node, AttrList config) {
		// do nothing
	}

	@Override
	public ViSysPropEnabled wrapSingle(DynNode node) {
		return new NodeWrapper(node);
	}

	@Override
	public ViSysPropEnabled wrapMultiple(NodeCallProxy group, DynNode[] nodes) {
		return new GroupWrapper(group);
	}

	public static class NodeWrapper implements ViSysPropEnabled, SysProps {

		private final DynNode host;
		
		public NodeWrapper(DynNode host) {
			this.host = host;
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.BY_IMPLEMENTATION)
		public SysProps sysProps() {
			return NodeSpiHelper.createDynamicFacade(SysProps.class, host);
		}

		@Override
		@MethodMode(deferNode=DeferingMode.SPI_REQUIRED, groupCallNode=GroupCallMode.UNSUPPORTED)
		public String get(final String propName) {
			return host.getProxy().exec(new Callable<String>() {
				@Override
				public String call() throws Exception {
					return System.getProperty(propName);
				}
			});
		}

		@Override
		@MethodMode(deferNode=DeferingMode.SPI_REQUIRED, groupCallNode=GroupCallMode.UNSUPPORTED)
		public Map<String, String> getAll(final Collection<String> props) {
			return host.getProxy().exec(new Callable<Map<String, String>>() {
				@Override
				public Map<String, String> call() throws Exception {
					Map<String, String> map = new LinkedHashMap<String, String>();
					for(Object key: props) {
						String prop = (String) key;
						map.put(prop, System.getProperty(prop));
					}
					return map;
				}
			});
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void put(final String propName, String prop) {
			putAll(Collections.singletonMap(propName, prop));
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void putAll(final Map<String, String> props) {
			final Runnable setProps = new Runnable() {
				@Override
				public void run() {
					System.getProperties().putAll(props);
				}
				
				@Override
				public String toString() {
					return "setProps" + props.toString();
				}
			};
			host.applyAction(new ViNodeAction() {
				@Override
				public void apply(ViNodeSpi nodeSPI) {
					try {
						nodeSPI.getExecutor().submit(setProps).get();
					} catch (Exception e) {
						if (e instanceof ExecutionException) {
							Any.throwUncheked(e.getCause());
						}
						else {
							Any.throwUncheked(e);
						}
					}
				}
			});
		}

		@Override
		@MethodMode(deferNode=DeferingMode.SPI_REQUIRED, groupCallNode=GroupCallMode.UNSUPPORTED)
		public Map<String, String> toMap() {
			return host.getProxy().exec(new Callable<Map<String, String>>() {
				@Override
				public Map<String, String> call() throws Exception {
					Map<String, String> map = new HashMap<String, String>();
					for(Object key: System.getProperties().keySet()) {
						String prop = (String) key;
						map.put(prop, System.getProperty(prop));
					}
					return map;
				}
			});
		}
	}
	
	public static class GroupWrapper implements ViSysPropEnabled {

		private final NodeCallProxy groupProxy;
		
		public GroupWrapper(NodeCallProxy groupProxy) {
			this.groupProxy = groupProxy;
		}

		@Override
		public SysProps sysProps() {
			return NodeSpiHelper.createDynamicFacade(SysProps.class, groupProxy);
		}
	}	
}
