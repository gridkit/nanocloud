package org.gridkit.vicluster;

import org.gridkit.nanocloud.ViConfigurable;
import org.gridkit.nanocloud.ViNodeControl;
import org.gridkit.zerormi.DirectRemoteExecutor;

public interface ViNodeCore extends ViConfigurable, ViNodeControl {

    public DirectRemoteExecutor executor();

}
