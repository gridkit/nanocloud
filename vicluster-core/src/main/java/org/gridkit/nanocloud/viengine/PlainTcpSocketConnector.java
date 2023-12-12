package org.gridkit.nanocloud.viengine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

import org.gridkit.zerormi.DuplexStream;

class PlainTcpSocketConnector implements RemoteEndPoint {

    private final String url;

    public PlainTcpSocketConnector(String url) {
        this.url = url;
    }

    @Override
    public DuplexStream connect() throws IOException {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                throw new URISyntaxException(url, "Failed to parse hostname");
            }
            int port = uri.getPort();

            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port));

            return new SocketDuplexStream(socket);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
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
        PlainTcpSocketConnector other = (PlainTcpSocketConnector) obj;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }

    private static class SocketDuplexStream implements DuplexStream {

        private final Socket socket;

        public SocketDuplexStream(Socket socket) {
            this.socket = socket;
        }

        @Override
        public InputStream getInput() throws IOException {
            return socket.getInputStream();
        }

        @Override
        public OutputStream getOutput() throws IOException {
            return socket.getOutputStream();
        }

        @Override
        public boolean isClosed() {
            return socket.isClosed();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
