package org.gridkit.nanocloud.telecontrol;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.zerormi.DirectRemoteExecutor;
import org.gridkit.zerormi.DuplexStream;
import org.gridkit.zerormi.DuplexStreamConnector;
import org.gridkit.zerormi.RemoteExecutor;
import org.gridkit.zerormi.RemoteExecutorAsynAdapter;
import org.gridkit.zerormi.RmiGateway;
import org.gridkit.zerormi.hub.RemotingEndPoint;
import org.gridkit.zerormi.hub.SlaveSpore;

public class ZeroRmiRemoteSession implements RemoteExecutionSession {

    private RmiGateway gateway;

    public ZeroRmiRemoteSession(String nodeName) {
        // TODO logging configuration
        gateway = new RmiGateway(nodeName);

    }

    @Override
    public SlaveSpore getMobileSpore() {
        Spore spore = new Spore();
        return spore;
    }

    @Override
    public DirectRemoteExecutor getRemoteExecutor() {
        FutureEx<RemoteExecutor> rf = gateway.getRemoteExecutionPoint();
        if (rf.isDone()) {
            try {
                return new RemoteExecutorAsynAdapter(rf.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else {
            return RemoteExecutorAsynAdapter.defered(rf);
        }
    }

    @Override
    public void setTransportConnection(DuplexStream stream) {
        try {
            gateway.connect(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void terminate(Throwable cause) {
        gateway.shutdown(cause);
    }

    public static class Spore implements SlaveSpore {

        private static final long serialVersionUID = 20130806L;

        @Override
        public void start(DuplexStreamConnector masterConnector) {
            RemotingEndPoint endpoint = new RemotingEndPoint(null, masterConnector);
            endpoint.enableHeartbeatDeatchWatch();
            endpoint.run();
        }

        @Override
        public String toString() {
            return "RemotingEndPoint";
        }
    }
}
