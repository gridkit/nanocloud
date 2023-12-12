package org.gridkit.nanocloud.viengine;

import java.io.IOException;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;

public interface RemoteHostConnector {

    public HostControlConsole connect() throws IOException;

}
