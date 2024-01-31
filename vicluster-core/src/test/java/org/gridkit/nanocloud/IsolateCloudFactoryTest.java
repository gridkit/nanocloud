package org.gridkit.nanocloud;

public class IsolateCloudFactoryTest extends CloudFactoryTest{

    @Override
    public Cloud initCloud() {
        Cloud cloud = Nanocloud.createCloud();
        cloud.x(VX.ISOLATE);
        return cloud;
    }
}
