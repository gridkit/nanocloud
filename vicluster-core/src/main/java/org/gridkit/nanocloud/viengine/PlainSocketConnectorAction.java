package org.gridkit.nanocloud.viengine;

import java.net.URI;
import java.net.URISyntaxException;

import org.gridkit.nanocloud.RemoteEx;

class PlainSocketConnectorAction extends AbstractNodeAction {

    public static final PlainSocketConnectorAction INSTANCE = new PlainSocketConnectorAction();

    private InArg<String> targetUrl = required("remote:target-url");
    private InArg<String> remoteCachePath = required(RemoteEx.JAR_CACHE_PATH);

    @Override
    protected void run() {
        String targetUrl = transform(this.targetUrl.get());
        try {
            URI uri = new URI(targetUrl);
            if ("tcp".equals(uri.getScheme())) {
                PlainTcpSocketConnector connector = new PlainTcpSocketConnector(targetUrl);
                TunnellerHostControlConnector tunneler = new TunnellerHostControlConnector(targetUrl, connector, remoteCachePath.get());
                getContext().set(Pragma.RUNTIME_HOST_CONNECTOR, tunneler);
            }
        } catch (URISyntaxException e) {
            BootAnnotation.fatal(getContext(), "Invalid URL %s - %s", targetUrl, e.toString());
        }
    }
}
