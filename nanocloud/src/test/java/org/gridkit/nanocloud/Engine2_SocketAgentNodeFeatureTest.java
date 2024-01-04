package org.gridkit.nanocloud;

import static org.gridkit.nanocloud.VX.REMOTE;

import org.gridkit.nanocloud.Nanocloud;
import org.gridkit.vicluster.telecontrol.agent.PlainSocketNanoAgent;
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
        cloud = Nanocloud.createCloud();
        cloud.x(REMOTE).setTargetUrl("tcp://127.0.0.1:12034");
    }
}
