package org.gridkit.nanocloud.viengine;

import java.io.IOException;

import org.gridkit.zerormi.DuplexStream;

public interface RemoteEndPoint {

    public DuplexStream connect() throws IOException;

}
