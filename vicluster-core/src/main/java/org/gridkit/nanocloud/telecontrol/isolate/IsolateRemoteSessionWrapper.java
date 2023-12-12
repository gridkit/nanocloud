package org.gridkit.nanocloud.telecontrol.isolate;

import org.gridkit.nanocloud.telecontrol.RemoteExecutionSession;
import org.gridkit.nanocloud.telecontrol.RemoteExecutionSessionWrapper;

/**
 * This wrapper will start remote slave in Isolate.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class IsolateRemoteSessionWrapper implements RemoteExecutionSessionWrapper {

    public static final IsolateRemoteSessionWrapper INSTANCE = new IsolateRemoteSessionWrapper();

    @Override
    public RemoteExecutionSession wrap(RemoteExecutionSession session) {
        return new IsolatedRemoteSession(session);
    }
}
