package org.gridkit.nanocloud;

import org.gridkit.nanocloud.viengine.ViEngine2NodeProvider;
import org.gridkit.vicluster.ViManager;

public class Nanocloud {

    public static Cloud createCloud() {

        ViEngine2NodeProvider provider = new ViEngine2NodeProvider();
        provider.initDefaultProviders();
        Cloud cloud = new ViManager(provider);

        return cloud;
    }

}
