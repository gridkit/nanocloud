package org.gridkit.nanocloud.telecontrol.agent;

import java.io.IOException;

import org.gridkit.nanocloud.agent.PlainSocketNanoAgent;
import org.gridkit.nanocloud.telecontrol.websock.agent.WebSocketNanoAgent;

public class NanoAgent {

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length < 1) {
            System.err.println("Server type required 'ws' or 'tcp'");
            System.exit(1);
        }
        if ("ws".equalsIgnoreCase(args[0])) {
            WebSocketNanoAgent.main(args);
        } else if ("tcp".equalsIgnoreCase(args[0])) {
            PlainSocketNanoAgent.main(args);
        } else {
            System.err.println("Server type required 'ws' or 'tcp'");
            System.exit(1);
        }
        if (args.length == 2) {
            int port = Integer.valueOf(args[1]);
            if (System.getProperty("nanoagent.port") == null) {
                System.setProperty("nanoagent.port", String.valueOf(port));
            }
        }
    }
}
