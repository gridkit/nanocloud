package org.gridkit.nanocloud.viengine;

import org.gridkit.nanocloud.Cloud;
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
