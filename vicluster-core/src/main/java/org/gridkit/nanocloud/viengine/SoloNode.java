package org.gridkit.nanocloud.viengine;

import org.gridkit.zerormi.DirectRemoteExecutor;

interface SoloNode {

    public DirectRemoteExecutor getExecutor();

}
