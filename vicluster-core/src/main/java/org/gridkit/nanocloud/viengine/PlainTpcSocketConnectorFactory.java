package org.gridkit.nanocloud.viengine;

import java.net.URI;
import java.net.URISyntaxException;

class PlainTpcSocketConnectorFactory implements EndPointConnectorFactory {

    public static final PlainTpcSocketConnectorFactory INSTANCE = new PlainTpcSocketConnectorFactory();

    @Override
    public RemoteEndPoint newConnector(String uri, ConfMap reader) {
        try {
            URI u = new URI(uri);
            if ("tcp".equals(u.getScheme())) {
                return new PlainTcpSocketConnector(uri);
            }
        } catch (URISyntaxException e) {
        }
        return null;
    }
}
