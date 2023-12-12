package org.gridkit.nanocloud.viengine;

import static org.gridkit.nanocloud.VX.LOCAL;

import org.gridkit.nanocloud.ViNodeFeatureTest;

public class Engine2_LocalNodeFeatureTest extends ViNodeFeatureTest {

    @Override
    public void initCloud() {
        cloud = Engine2.createCloud();
        cloud.x(LOCAL);
    }
}
