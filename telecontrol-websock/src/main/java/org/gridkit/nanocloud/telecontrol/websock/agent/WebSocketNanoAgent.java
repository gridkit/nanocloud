package org.gridkit.nanocloud.telecontrol.websock.agent;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.gridkit.vicluster.telecontrol.agent.NanoAgent;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoWSD;
import fi.iki.elonen.NanoWSD.WebSocket;
import fi.iki.elonen.NanoWSD.WebSocketFrame;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;

public class WebSocketNanoAgent extends NanoAgent {

    public static void main(String[] args) throws InterruptedException, IOException {
        Integer port = Integer.getInteger("nanoagent.port");
        if (port == null) {
            throw new IllegalArgumentException("Port is required, add -Dnanoagent.port=<port> option");
        }

        WebSocketNanoAgent agent = new WebSocketNanoAgent(null, port);
        agent.start();
    }

    private final AtomicInteger counter = new AtomicInteger();
    private final NanoWSD nanoServer;

    public WebSocketNanoAgent(String hostname, int port) {
        nanoServer = new NanoWSD(hostname, port) {

            @Override
            protected Response serveHttp(final IHTTPSession session) {
                if (session.getUri().length() == 0 || "/".equals(session.getUri())) {
                    return serveBanner(session);
                } else {
                    return super.serveHttp(session);
                }
            }

            @Override
            protected WebSocket openWebSocket(IHTTPSession handshake) {
                return new SocketSession(handshake);
            }
        };
    }

    public void start() throws IOException {
        nanoServer.start();
    }

    public void stop() {
        nanoServer.stop();
    }

    private Response serveBanner(final IHTTPSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("This is NanoCloud agent end point. Use websocket transport to connect.");
        return NanoHTTPD.newFixedLengthResponse(sb.toString());
    }

    protected void log(String message) {
        System.out.println(message);
    }


    private class SocketSession extends WebSocket {

        final String id;
        final String remoteInfo;
        final StreamPipe inbound = new StreamPipe(64 << 10);
        final OutStream sender = new OutStream();
        volatile Closeable session;
        volatile boolean closed = false;

        SocketSession(IHTTPSession handshake) {
            super(handshake);
            this.id = "WS" + counter.getAndIncrement();
            this.remoteInfo = handshake.getHeaders().get("http-client-ip");
        }

        @Override
        protected void onOpen() {
            log("[" + id + "] New session from " + remoteInfo);
            session = start(remoteInfo, inbound.getInputStream(), sender);
        }

        @Override
        protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
            if (!closed) {
                closed = true;
                log("[" + id + "] Terminaled session from " + remoteInfo);
            }
            if (session != null) {
                try {
                    session.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            try {
                inbound.getOutputStream().close();
            } catch (IOException e) {
                // ignore
            }
        }

        @Override
        protected void onMessage(WebSocketFrame message) {
            try {
                inbound.getOutputStream().write(message.getBinaryPayload());
            } catch (IOException e) {
                log("[" + id + "] Exception - " + e.toString());
            }
        }

        @Override
        protected void onPong(WebSocketFrame pong) {
            // do nothing
        }

        @Override
        protected void onException(IOException exception) {
            // do nothing
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
                if (off != 0 || len != b.length) {
                    b = Arrays.copyOfRange(b, off, len);
                }
                send(b);
            }

            @Override
            public void close() throws IOException {
                SocketSession.this.close(CloseCode.AbnormalClosure, "close", false);
            }
        }
    }
}
