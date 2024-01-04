package org.gridkit.nanocloud;

import static org.gridkit.nanocloud.VX.ISOLATE;

import org.gridkit.nanocloud.Nanocloud;
import org.junit.Assume;

public class Engine2_IsolateNodeFeatureTest extends ViNodeFeatureTest {

    @Override
    protected void assumeOutOfProcess() {
        Assume.assumeTrue(false);
    }

    @Override
    public void initCloud() {
        cloud = Nanocloud.createCloud();
        cloud.x(ISOLATE);
    }
}
