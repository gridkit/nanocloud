package org.gridkit.nanocloud.telecontrol.ssh;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.SimpleTunnelInitiator;
import org.gridkit.nanocloud.telecontrol.TunnellerInitiator;
import org.gridkit.nanocloud.telecontrol.ssh.SshConfigurer.PropProvider;
import org.gridkit.nanocloud.viengine.AbstractNodeAction;
import org.gridkit.nanocloud.viengine.BootAnnotation;
import org.gridkit.nanocloud.viengine.Pragma;
import org.gridkit.nanocloud.viengine.RemoteHostConnector;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.telecontrol.StreamCopyService;
import org.gridkit.vicluster.telecontrol.ssh.SimpleSshSessionProvider;
import org.gridkit.vicluster.telecontrol.ssh.SshHostControlConsole;
import org.gridkit.zerormi.zlog.ZLogFactory;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

class SshConnectorAction extends AbstractNodeAction {

    public static final SshConnectorAction INSTANCE = new SshConnectorAction();

    private InArg<String> targetUrl = required("remote:target-url");


    @Override
    protected void run() {
        String targetUrl = transform(this.targetUrl.get());
        try {
            URI uri = new URI(targetUrl);
            if ("ssh".equals(uri.getScheme())) {
                RemoteHostConnector connector = createSshConnector();
                getContext().set(Pragma.RUNTIME_HOST_CONNECTOR, connector);
            }
        } catch (URISyntaxException e) {
            BootAnnotation.fatal(getContext(), "Invalid URL %s - %s", targetUrl, e.toString());
        }
    }

    private RemoteHostConnector createSshConnector() {

        SshConfigurer sshConfig = new SshConfigurer();
        sshConfig.init(new PropWrapper());

        return new SshSocketConnector(sshConfig, getContext().<StreamCopyService>get(ViConf.SPI_STREAM_COPY_SERVICE));
    }

    class PropWrapper implements PropProvider {

        @Override
        public String get(String prop) {
            return transform((String) getContext().get(prop));
        }

        @Override
        public List<String> keys(String prefix) {
            return getContext().match(prefix + "**");
        }
    }

    static class SshSocketConnector implements RemoteHostConnector {

        private final SshConfigurer sshConfig;
        private final StreamCopyService copyService;

        public SshSocketConnector(SshConfigurer sshConfig, StreamCopyService copyService) {
            this.sshConfig = sshConfig;
            this.copyService = copyService;
        }

        @Override
        public HostControlConsole connect() throws IOException {

            SimpleSshSessionProvider provider = new SimpleSshSessionProvider();

            sshConfig.configure(provider);

            Session session;
            try {
                session = provider.getSession(sshConfig.getRemoteHost(), sshConfig.getRemoteAccount());
            } catch (JSchException e) {
                throw new IOException(e);
            }
            String bootCachePath = sshConfig.getBootCachePath();
            String bootCmd = sshConfig.getBootCmd();

            SshHostControlConsole shhConsole = new SshHostControlConsole(session, bootCachePath, true, 1);
            // TODO logger configuration
            TunnellerInitiator initiator = new SimpleTunnelInitiator(bootCmd, copyService, ZLogFactory.getDefaultRootLogger());
            HostControlConsole console = initiator.initTunnel(shhConsole);

            return console;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((sshConfig == null) ? 0 : sshConfig.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SshSocketConnector other = (SshSocketConnector) obj;
            if (sshConfig == null) {
                if (other.sshConfig != null)
                    return false;
            } else if (!sshConfig.equals(other.sshConfig))
                return false;
            return true;
        }
    }
}
