package org.gridkit.nanocloud.test.junit;

import java.util.Collection;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.Nanocloud;
import org.gridkit.nanocloud.ViConfExtender;
import org.gridkit.nanocloud.ViNode;
import org.junit.rules.ExternalResource;

public class DisposableCloud extends ExternalResource implements CloudRule {

    private Cloud cloud = Nanocloud.createCloud();

    @Override
    protected void after() {
        shutdown();
    }

    public ViNode all() {
        return node("**");
    }

    @Override
    public <X> X x(ViConfExtender<X> extender) {
        return node("**").x(extender);
    }

    @Override
    public ViNode node(String nameOrSelector) {
        return cloud.node(nameOrSelector);
    }

    @Override
    public ViNode nodes(String... nameOrSelector) {
        return cloud.nodes(nameOrSelector);
    }

    @Override
    public Collection<ViNode> listNodes(String nameOrSelector) {
        return cloud.listNodes(nameOrSelector);
    }

    @Override
    public void shutdown() {
        cloud.shutdown();
    }
}
