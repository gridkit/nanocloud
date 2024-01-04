package org.gridkit.nanocloud.telecontrol.websock;

import static org.gridkit.nanocloud.VX.REMOTE;

import org.gridkit.nanocloud.RemoteEx;
import org.gridkit.nanocloud.viengine.Pragma;
import org.gridkit.vicluster.ViConfExtender;
import org.gridkit.vicluster.ViConfigurable;

public class WebSockConf extends RemoteEx<WebSockConf> {

    public static final ViConfExtender<WebSockConf> WEBSOCK = new ViConfExtender<WebSockConf>() {

        @Override
        public WebSockConf wrap(ViConfigurable node) {
            WebSockConf ssh = new WebSockConf(node);
            node.x(REMOTE);
            node.setConfigElement(Pragma.REMOTE_PROTOCOL_CONNECTOR + "ws", WebSocketConnectorFactory.INSTANCE);
            node.setConfigElement(Pragma.REMOTE_PROTOCOL_CONNECTOR + "wss", WebSocketConnectorFactory.INSTANCE);

            return ssh;
        }
    };

    public static final String REMOTE_WS_HEADER = "remote:web-sock-header-";

    protected WebSockConf(ViConfigurable target) {
        super(target);
    }

    public WebSockConf configureSimpleRemoting() {
        setRemoteUrl("~ws://%s!(.*)");
        return this;
    }

    public WebSockConf addHeader(String key, String value) {
        setConfigElement(REMOTE_WS_HEADER + key, value);
        return this;
    }


}
