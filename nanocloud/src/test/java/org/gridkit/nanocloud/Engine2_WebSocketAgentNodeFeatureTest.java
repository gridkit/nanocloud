package org.gridkit.nanocloud;

import static org.gridkit.nanocloud.telecontrol.websock.WebSockConf.WEBSOCK;

import java.io.IOException;

import org.gridkit.nanocloud.telecontrol.websock.agent.WebSocketNanoAgent;
import org.junit.After;
import org.junit.Before;

public class Engine2_WebSocketAgentNodeFeatureTest extends ViNodeFeatureTest {

    private WebSocketNanoAgent agent;

    @Before
    public void initAgent() throws IOException {
        agent = new WebSocketNanoAgent(null, 12035);
        agent.start();
    }

    @After
    public void stopAgent() {
        agent.stop();
    }

    @Override
    public void initCloud() {
        cloud = Nanocloud.createCloud();
        cloud.x(WEBSOCK).setTargetUrl("ws://127.0.0.1:12035");
        cloud.x(WEBSOCK)
            .addHeader("dummy1", "x")
            .addHeader("dummy2", "y");
    }
}
