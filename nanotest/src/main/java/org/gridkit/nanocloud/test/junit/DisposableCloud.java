package org.gridkit.nanocloud.test.junit;

import java.util.Collection;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.vicluster.ViNode;
import org.junit.rules.ExternalResource;

public class DisposableCloud extends ExternalResource implements CloudRule {

	private Cloud cloud = CloudFactory.createCloud();
	
	@Override
	protected void after() {
		shutdown();
	}

	public ViNode all() {
		return node("**");
	}

	public ViNode node(String nameOrSelector) {
		return cloud.node(nameOrSelector);
	}

	public ViNode nodes(String... nameOrSelector) {
		return cloud.nodes(nameOrSelector);
	}

	public Collection<ViNode> listNodes(String nameOrSelector) {
		return cloud.listNodes(nameOrSelector);
	}

	public void shutdown() {
		cloud.shutdown();
	}
}
