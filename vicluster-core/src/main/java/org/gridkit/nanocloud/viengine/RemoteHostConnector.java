package org.gridkit.nanocloud.viengine;

import java.io.IOException;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;

/**
 * Abstracted way to connect to nanoagent.
 * <p>
 * Implementation is expected to implement {@link #equals(Object)} and {@link #hashCode()},
 * so {@link HostControlConsole} could be reused in case of multiple nodes on same host.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface RemoteHostConnector {

    public HostControlConsole connect() throws IOException;

}
