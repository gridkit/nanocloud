package org.gridkit.nanocloud.telecontrol.websock;

import org.gridkit.nanocloud.viengine.ConfMap;
import org.gridkit.nanocloud.viengine.EndPointConnectorFactory;
import org.gridkit.nanocloud.viengine.RemoteEndPoint;

public class WebSocketConnectorFactory implements EndPointConnectorFactory {

    public static final WebSocketConnectorFactory INSTANCE = new WebSocketConnectorFactory();

    @Override
    public RemoteEndPoint newConnector(String uri, ConfMap conf) {
        WebSocketConnector connector = new WebSocketConnector(uri);
        for (String hdr: conf.match(WebSockConf.REMOTE_WS_HEADER + "**")) {
            String key = hdr.substring(WebSockConf.REMOTE_WS_HEADER.length());
            String val = conf.get(hdr);
            connector.addHeader(key, val);
        }
        return connector;
    }
}
