package org.gridkit.vicluster.spi;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import org.gridkit.vicluster.isolate.IsolateViNode;
import org.gridkit.vicluster.isolate.Isolation;
import org.gridkit.vicluster.spi.NodeSpiHelper.MethodMode;

public class IsolateViNodeExtention implements ViCloudExtention<IsolateViNode>{

	@Override
	public Class<IsolateViNode> getFacadeInterface() {
		return IsolateViNode.class;
	}

	@Override
	public Class<?>[] getHidenInterfaces() {
		return new Class<?>[]{Isolation.class};
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
	}

	@Override
	public IsolateViNode wrapSingle(DynNode node) {
		return new NodeWrapper(node);
	}

	@Override
	public IsolateViNode wrapMultiple(NodeCallProxy selector, DynNode[] nodes) {
		return new GroupWrapper(selector);
	}

	private static void checkURL(String url) {		
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	private static class GroupWrapper extends ViNodeStub implements IsolateViNode {
		
		private final NodeCallProxy proxy;

		public GroupWrapper(NodeCallProxy proxy) {
			this.proxy = proxy;
		}
		
		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.BY_IMPLEMENTATION)
		public Isolation isolation() {
			return NodeSpiHelper.createDynamicFacade(Isolation.class, proxy);
		}
	}

	private static class NodeWrapper extends ViNodeStub implements IsolateViNode, Isolation {
		
		private final DynNode dynNode;

		public NodeWrapper(DynNode dynNode) {
			this.dynNode = dynNode;
		}
		
		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.BY_IMPLEMENTATION)
		public Isolation isolation() {
			return NodeSpiHelper.createDynamicFacade(Isolation.class, dynNode);
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void includePackage(String packageName) {
			dynNode.applyAction(new IsolatePackageAction(packageName));
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void excludeClass(Class<?> type) {
			dynNode.applyAction(new ExcludeTypeAction(type.getName()));
		}		

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void excludeClass(String className) {
			dynNode.applyAction(new ExcludeTypeAction(className));
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void addToClasspath(String url) {
			dynNode.applyAction(new AddPathAction(url));
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void addToClasspath(URL url) {
			dynNode.applyAction(new AddPathAction(url.toString()));
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void removeFromClasspath(String url) {
			dynNode.applyAction(new RemovePathAction(url));
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void removeFromClasspath(URL url) {
			dynNode.applyAction(new RemovePathAction(url.toString()));
		}
	}
	
	private static class IsolatePackageAction implements ViNodeAction, Serializable {
		
		private static final long serialVersionUID = 20120925L;
		
		private final String packageName;

		public IsolatePackageAction(String packageName) {
			this.packageName = packageName;
		}

		@Override
		public void apply(ViNodeSpi nodeSPI) {
			IsolateViNodeSpi isolate = nodeSPI.adapt(IsolateViNodeSpi.class);
			if (isolate != null) {
				isolate.includePackage(packageName);
			}
		}
	}

	private static class ExcludeTypeAction implements ViNodeAction, Serializable {
		
		private static final long serialVersionUID = 20120925L;
		
		private final String className;
		
		public ExcludeTypeAction(String className) {
			this.className = className;
		}
		
		@Override
		public void apply(ViNodeSpi nodeSPI) {
			IsolateViNodeSpi isolate = nodeSPI.adapt(IsolateViNodeSpi.class);
			if (isolate != null) {
				isolate.excludeClass(className);
			}
		}
	}

	private static class AddPathAction implements ViNodeAction, Serializable {
		
		private static final long serialVersionUID = 20120925L;
		
		private final String path;
		
		public AddPathAction(String path) {
			checkURL(path);
			this.path = path;
		}
		
		@Override
		public void apply(ViNodeSpi nodeSPI) {
			IsolateViNodeSpi isolate = nodeSPI.adapt(IsolateViNodeSpi.class);
			if (isolate != null) {
				isolate.addToClasspath(path);
			}
		}
	}

	private static class RemovePathAction implements ViNodeAction, Serializable {
		
		private static final long serialVersionUID = 20120925L;
		
		private final String path;
		
		public RemovePathAction(String path) {
			checkURL(path);
			this.path = path;
		}
		
		@Override
		public void apply(ViNodeSpi nodeSPI) {
			IsolateViNodeSpi isolate = nodeSPI.adapt(IsolateViNodeSpi.class);
			if (isolate != null) {
				isolate.removeFromClasspath(path);
			}
		}
	}
}
