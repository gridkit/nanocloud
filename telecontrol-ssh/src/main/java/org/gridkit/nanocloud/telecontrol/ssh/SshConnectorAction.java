package org.gridkit.nanocloud.telecontrol.ssh;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.SimpleTunnelInitiator;
import org.gridkit.nanocloud.telecontrol.TunnellerInitiator;
import org.gridkit.nanocloud.telecontrol.ssh.SshConfigurer.PropProvider;
import org.gridkit.nanocloud.viengine.AbstractNodeAction;
import org.gridkit.nanocloud.viengine.BootAnnotation;
import org.gridkit.nanocloud.viengine.Pragma;
import org.gridkit.nanocloud.viengine.RemoteHostConnector;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.telecontrol.FileBlob;
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

            SshHostControlConsole sshConsole = new SshHostControlConsole(session, bootCachePath, true, 1);
            // TODO logger configuration
            TunnellerInitiator initiator = new SimpleTunnelInitiator(bootCmd, copyService, ZLogFactory.getDefaultRootLogger());
            HostControlConsole console = initiator.initTunnel(sshConsole);
            CoupledHostConsole cc = new CoupledHostConsole(sshConsole, console);

            return cc;
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

    private static class CoupledHostConsole implements HostControlConsole {

        private final HostControlConsole bootConsole;
        private final HostControlConsole console;

        public CoupledHostConsole(HostControlConsole bootConsole, HostControlConsole console) {
            this.bootConsole = bootConsole;
            this.console = console;
        }

        @Override
        public String getHostname() {
            return console.getHostname();
        }

        @Override
        public boolean isLocalFileSystem() {
            return console.isLocalFileSystem();
        }

        @Override
        public String cacheFile(FileBlob blob) {
            return console.cacheFile(blob);
        }

        @Override
        public List<String> cacheFiles(List<? extends FileBlob> blobs) {
            return console.cacheFiles(blobs);
        }

        @Override
        public Destroyable openSocket(SocketHandler handler) {
            return console.openSocket(handler);
        }

        @Override
        public Destroyable startProcess(String workDir, String[] command, Map<String, String> env,
                ProcessHandler handler) {
            return console.startProcess(workDir, command, env, handler);
        }

        @Override
        public void terminate() {
            console.terminate();
            bootConsole.terminate();
        }
    }
}
