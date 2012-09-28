package org.gridkit.vicluster.spi;

import org.gridkit.vicluster.RemoteDetails;
import org.gridkit.vicluster.RemoteViNode;
import org.gridkit.vicluster.spi.NodeSpiHelper.MethodMode;

public class RemoteNodeExtentions extends AbstractViCloudExtention<RemoteViNode> {

	public RemoteNodeExtentions() {
		super(RemoteViNode.class, RemoteWrapper.class, RemoteDetails.class);
	}
	
	@Override
	public void processNodeConfig(DynNode node, AttrList config) {
		RemoteWrapper wrapper = (RemoteWrapper) node.adapt(RemoteViNode.class);
		config.addAll(wrapper.list);
	}

	@Override
	public RemoteViNode wrapSingle(DynNode node) {
		return new RemoteWrapper(node);
	}

	@Override
	public RemoteViNode wrapMultiple(NodeCallProxy selector, DynNode[] nodes) {
		return new GroupWrapper(selector);
	}
	
	private static class RemoteWrapper extends ViNodeStub implements RemoteViNode, RemoteDetails {

		private final DynNode node;
		private AttrList list = new AttrList();

		public RemoteWrapper(DynNode node) {
			this.node = node;
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.BY_IMPLEMENTATION)
		public RemoteDetails remote() {
			return NodeSpiHelper.createDynamicFacade(RemoteDetails.class, node);
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void host(String host) {
			list.add(RemoteAttrs.NODE_HOST, host);
			
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void hostGroup(String hostgroup) {
			list.add(RemoteAttrs.NODE_HOSTGROUP, hostgroup);			
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void hostGroup(String hostgroup, String colocId) {
			list.add(RemoteAttrs.NODE_HOSTGROUP, hostgroup);			
			list.add(RemoteAttrs.NODE_COLOCATION_ID, colocId);			
		}

		@Override
		@MethodMode(deferNode=DeferingMode.NO_SPI_NEEDED, groupCallNode=GroupCallMode.STICKY_BROADCAST)
		public void colocId(String id) {
			list.add(RemoteAttrs.NODE_COLOCATION_ID, id);			
		}
	}
	
	public static class GroupWrapper extends ViNodeStub implements RemoteViNode {

		private final NodeCallProxy groupProxy;
		
		public GroupWrapper(NodeCallProxy groupProxy) {
			this.groupProxy = groupProxy;
		}

		@Override
		public RemoteDetails remote() {
			return NodeSpiHelper.createDynamicFacade(RemoteDetails.class, groupProxy);
		}
	}		
}
