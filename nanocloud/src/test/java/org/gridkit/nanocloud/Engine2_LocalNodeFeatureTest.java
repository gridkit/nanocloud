package org.gridkit.nanocloud;

import static org.gridkit.nanocloud.VX.LOCAL;

import org.gridkit.nanocloud.Nanocloud;

public class Engine2_LocalNodeFeatureTest extends ViNodeFeatureTest {

    @Override
    public void initCloud() {
        cloud = Nanocloud.createCloud();
        cloud.x(LOCAL);
    }
}
