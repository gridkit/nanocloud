package org.gridkit.nanocloud.viengine;

import static org.gridkit.nanocloud.VX.REMOTE;

import org.gridkit.nanocloud.ViNodeFeatureTest;
import org.gridkit.vicluster.telecontrol.bootstraper.PlainSocketNanoAgent;
import org.junit.After;
import org.junit.Before;

public class Engine2_SocketAgentNodeFeatureTest extends ViNodeFeatureTest {

    private PlainSocketNanoAgent agent;

    @Before
    public void initAgent() {
        agent = new PlainSocketNanoAgent();
        agent.setPort(12034);
        agent.start();
    }

    @After
    public void stopAgent() {
        agent.stop();
    }

    @Override
    public void initCloud() {
        cloud = Engine2.createCloud();
        cloud.x(REMOTE).setTargetUrl("tcp://127.0.0.1:12034");
    }
}
