package org.gridkit.nanocloud.viengine;

import java.net.URI;
import java.net.URISyntaxException;

import org.gridkit.nanocloud.RemoteEx;

class EndPointConnectorAction extends AbstractNodeAction {

    public static final EndPointConnectorAction INSTANCE = new EndPointConnectorAction();

    private InArg<String> targetUrl = required(RemoteEx.REMOTE_TARGET_URL);
    private InArg<String> remoteCachePath = required(RemoteEx.JAR_CACHE_PATH);

    @Override
    protected void run() {
        String targetUrl = transform(this.targetUrl.get());
        try {
            URI uri = new URI(targetUrl);
            String scheme = uri.getScheme().toLowerCase();
            EndPointConnectorFactory factory = getContext().get(Pragma.REMOTE_PROTOCOL_CONNECTOR + scheme);
            if (factory != null) {
                RemoteEndPoint connector = factory.newConnector(targetUrl, getContext());
                TunnellerHostControlConnector tunneler = new TunnellerHostControlConnector(targetUrl, connector, remoteCachePath.get());
                getContext().set(Pragma.RUNTIME_HOST_CONNECTOR, tunneler);
            }
        } catch (URISyntaxException e) {
            BootAnnotation.fatal(getContext(), "Invalid URL %s - %s", targetUrl, e.toString());
        }
    }
}
