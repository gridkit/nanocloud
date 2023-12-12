package org.gridkit.nanocloud.viengine;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;

public class TunnlerInitAction extends AbstractNodeAction {

    public static final TunnlerInitAction INSTANCE = new TunnlerInitAction();

    private InArg<RemoteHostConnector> connnector = required(Pragma.RUNTIME_HOST_CONNECTOR);

    private InArg<RemoteControlConnectionManager> connectionManager = required(Pragma.RUNTIME_REMOTE_CONNECTION_MANAGE);

    @Override
    protected void run() {
        final HostControlConsole console = connectionManager.get().open(connnector.get());
        getContext().set(Pragma.RUNTIME_HOST_CONTROL_CONSOLE, console);
        NodeConfigHelper.addFinalizer(getContext(), Pragma.RUNTIME_HOST_CONTROL_CONSOLE, new Runnable() {

            @Override
            public void run() {
                console.terminate();
            }
        });
    }
}
