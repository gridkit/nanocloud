package org.gridkit.nanocloud.telecontrol.websock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gridkit.nanocloud.viengine.RemoteEndPoint;
import org.gridkit.vicluster.telecontrol.StreamPipe;
import org.gridkit.zerormi.DuplexStream;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class WebSocketConnector implements RemoteEndPoint {

    private final String url;
    private final Map<String, String> headers = new LinkedHashMap<String, String>();

    public WebSocketConnector(String url) {
        this.url = url;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    @Override
    public DuplexStream connect() throws IOException {
        try {
            URI uri = new URI(url);
            return new WebSockerDuplexStream(uri, headers);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((headers == null) ? 0 : headers.hashCode());
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
        WebSocketConnector other = (WebSocketConnector) obj;
        if (headers == null) {
            if (other.headers != null)
                return false;
        } else if (!headers.equals(other.headers))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        return true;
    }

    private static class WebSockerDuplexStream extends WebSocketClient implements DuplexStream {

        private final StreamPipe inbound = new StreamPipe(64 << 10);
        private final OutStream outbound = new OutStream();

        public WebSockerDuplexStream(URI uri, Map<String, String> headers) throws InterruptedException {
            super(uri, headers);
            connectBlocking();
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            // do nothing
        }

        @Override
        public void onMessage(String message) {
            throw new IllegalArgumentException("Binary protocol expected");
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            try {
                byte[] data = new byte[bytes.remaining()];
                bytes.get(data);
                inbound.getOutputStream().write(data);
            } catch (IOException e) {
                e.printStackTrace(); // StreamPipe should never throw this
                close();
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            try {
                inbound.getOutputStream().close();
            } catch (IOException e) {
                // ignore
            }
        }

        @Override
        public void onError(Exception ex) {
            try {
                inbound.getOutputStream().close();
            } catch (IOException e) {
                // ignore
            }
        }

        @Override
        public InputStream getInput() throws IOException {
            return inbound.getInputStream();
        }

        @Override
        public OutputStream getOutput() throws IOException {
            return outbound;
        }

        @Override
        public void close() {
            super.close();
        }

        private class OutStream extends OutputStream {

            @Override
            public void write(int b) throws IOException {
                write(new byte[] {(byte) b});
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                send(ByteBuffer.wrap(b, off, len));
            }

            @Override
            public void close() throws IOException {
                WebSockerDuplexStream.this.close();
            }
        }
    }
}
