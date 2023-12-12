package org.gridkit.nanocloud.viengine;

import static org.gridkit.nanocloud.VX.ISOLATE;

import org.gridkit.nanocloud.ViNodeFeatureTest;
import org.junit.Assume;

public class Engine2_IsolateNodeFeatureTest extends ViNodeFeatureTest {

    @Override
    protected void assumeOutOfProcess() {
        Assume.assumeTrue(false);
    }

    @Override
    public void initCloud() {
        cloud = Engine2.createCloud();
        cloud.x(ISOLATE);
    }
}
