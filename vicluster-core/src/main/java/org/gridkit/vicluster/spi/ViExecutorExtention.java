package org.gridkit.vicluster.spi;

import java.lang.reflect.Method;

import org.gridkit.util.concurrent.AdvancedExecutor;
import org.gridkit.vicluster.ViExecutor;

class ViExecutorExtetion implements ViCloudExtention<ViExecutor> {

	@Override
	public Class<ViExecutor> getFacadeInterface() {
		return ViExecutor.class;
	}
	
	@Override
	public Class<?>[] getHidenInterfaces() {
		return new Class[0];
	}

	@Override
	public DeferingMode deferingModeForMethod(Method m) {
		if (m.getName().equals("exec") || m.getName().equals("massExec")) {
			return DeferingMode.SPI_REQUIRED;
		}
		else {
			return DeferingMode.SMART_DEFERABLE;
		}
	}

	@Override
	public GroupCallMode groupModeForMethod(Method m) {
		return GroupCallMode.BY_IMPLEMENTATION;
	}

	@Override
	public void processNodeConfig(DynNode node, AttrList config) {
		// do nothing
	}

	@Override
	public ViExecutor wrapSingle(DynNode node) {
		return new SingleNodeViExecutor(node);
	}

	@Override
	public ViExecutor wrapMultiple(NodeCallProxy group, DynNode[] nodes) {
		ViNodeSpi[] coreNodes = new ViNodeSpi[nodes.length];
		for(int i = 0; i != nodes.length; ++i) {
			coreNodes[i] = nodes[i].getCoreNode();
		}
		return new MultipleNodeViExecutor(coreNodes);
	}
	
	private static class SingleNodeViExecutor extends AbstractSingleNodeExecutor {
		
		private DynNode dynNode;

		public SingleNodeViExecutor(DynNode dynNode) {
			this.dynNode = dynNode;
		}

		@Override
		protected AdvancedExecutor getExecutor() {
			return dynNode.getCoreNode().getExecutor();
		}
	}

	private static class MultipleNodeViExecutor extends AbstractMultipleNodeExecutor {
		
		private AdvancedExecutor[] executors;
		
		public MultipleNodeViExecutor(ViNodeSpi[] coreNodes) {
			this.executors = new AdvancedExecutor[coreNodes.length];
			for(int i = 0; i != coreNodes.length; ++i) {
				executors[i] = coreNodes[i].getExecutor();
			}
		}

		@Override
		protected AdvancedExecutor[] getExecutors() {
			return executors;
		}
	}
}
