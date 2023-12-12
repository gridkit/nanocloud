package org.gridkit.nanocloud.viengine;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.TunnellerControlConsole;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection;
import org.gridkit.zerormi.DuplexStream;

public class TunnellerHostControlConnector implements RemoteHostConnector {

    private final String url;
    private final RemoteEndPoint connector;
    private final String remoteCachePath;

    public TunnellerHostControlConnector(String url, RemoteEndPoint connector, String remoteCachePath) {
        assert url != null;
        this.url = url;
        this.connector = connector;
        this.remoteCachePath = remoteCachePath;
    }

    @Override
    public HostControlConsole connect() throws IOException {

        try {
            DuplexStream stream = connector.connect();

            TunnellerConnection connection = new TunnellerConnection(url, stream.getInput(), stream.getOutput(), System.out, 10, TimeUnit.SECONDS);
            TunnellerControlConsole console = new TunnellerControlConsole(connection, remoteCachePath);
            return console;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((connector == null) ? 0 : connector.hashCode());
        result = prime * result + ((remoteCachePath == null) ? 0 : remoteCachePath.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TunnellerHostControlConnector other = (TunnellerHostControlConnector) obj;
        if (connector == null) {
            if (other.connector != null)
                return false;
        } else if (!connector.equals(other.connector))
            return false;
        if (remoteCachePath == null) {
            if (other.remoteCachePath != null)
                return false;
        } else if (!remoteCachePath.equals(other.remoteCachePath))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }
}
