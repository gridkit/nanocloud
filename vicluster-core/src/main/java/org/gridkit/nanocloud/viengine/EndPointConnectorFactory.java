package org.gridkit.nanocloud.viengine;

public interface EndPointConnectorFactory {

    RemoteEndPoint newConnector(String uri, ConfMap conf);

}
