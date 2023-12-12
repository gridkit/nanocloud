package org.gridkit.vicluster.isolate;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.viengine.IsolateNodeTypeInitializer;
import org.gridkit.nanocloud.viengine.LocalNodeTypeInitializer;
import org.gridkit.nanocloud.viengine.RemoteNodeTypeInitializer;
import org.gridkit.nanocloud.viengine.ViEngine2NodeProvider;
import org.gridkit.vicluster.ViManager;

public class Engine2 {

    public static Cloud createCloud() {
        return new ViManager(
                new ViEngine2NodeProvider()
                .addTypeInitializer("local", LocalNodeTypeInitializer.class)
                .addTypeInitializer("isolate", IsolateNodeTypeInitializer.class)
                .addTypeInitializer("remote", RemoteNodeTypeInitializer.class)
        );
    }

}
