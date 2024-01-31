package org.gridkit.nanocloud;

import java.util.Arrays;
import java.util.Collection;

import org.gridkit.nanocloud.viengine.ViManager2;

/**
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface Cloud {

    /**
     * Return node by name (or group of nodes for pattern).
     */
    public ViNode node(String nameOrSelector);

    public ViNode nodes(String... nameOrSelector);

    /**
     * Syntax sugar for
     * <code>
     * 		code.nodes("**").x(...);
     * </code>
     */
    public <X> X x(ViConfExtender<X> extender);

    /**
     * List non-terminated nodes matching namePattern
     */
    public Collection<ViNode> listNodes(String nameOrSelector);

    public void shutdown();

    public static ViNode multiNode(Collection<ViNode> nodes) {
        return ViManager2.multiNode(nodes);
    }

    public static ViNode multiNode(ViNode... nodes) {
        return multiNode(Arrays.asList(nodes));
    }
}
