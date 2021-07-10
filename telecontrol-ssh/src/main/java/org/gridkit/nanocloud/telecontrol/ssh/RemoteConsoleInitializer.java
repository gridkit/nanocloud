package org.gridkit.nanocloud.telecontrol.ssh;

import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_JSCH_OPTION;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_PASSWORD;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SPI_SSH_PRIVATE_KEY_FILE;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SSH_PASSWORD;
import static org.gridkit.nanocloud.telecontrol.ssh.SshSpiConf.SSH_PRIVATE_KEY_FILE;

import java.util.List;
import java.util.Map;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.gridkit.nanocloud.telecontrol.HostControlConsole;
import org.gridkit.nanocloud.telecontrol.SimpleTunnelInitiator;
import org.gridkit.nanocloud.telecontrol.TunnellerInitiator;
import org.gridkit.vicluster.CloudContext;
import org.gridkit.vicluster.CloudContext.ServiceKey;
import org.gridkit.vicluster.CloudContext.ServiceProvider;
import org.gridkit.vicluster.ViConf;
import org.gridkit.vicluster.ViEngine.InductiveRule;
import org.gridkit.vicluster.ViEngine.QuorumGame;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.gridkit.vicluster.telecontrol.StreamCopyService;
import org.gridkit.vicluster.telecontrol.ssh.SimpleSshSessionProvider;
import org.gridkit.vicluster.telecontrol.ssh.SshHostControlConsole;
import org.gridkit.zerormi.zlog.ZLogFactory;

public class RemoteConsoleInitializer implements InductiveRule {

    @Override
    public boolean apply(QuorumGame game) {
        if (game.get(SshSpiConf.SPI_SSH_TARGET_HOST) != null && game.get(SshSpiConf.SPI_SSH_TARGET_ACCOUNT) != null) {
            // TODO more accurate verification of preconditions
            game.setProp(ViConf.SPI_CONTROL_CONSOLE, initConsole(game));
            if (game.get(ViConf.JVM_EXEC_CMD) == null) {
                game.setProp(ViConf.JVM_EXEC_CMD, resolveBootCmd(game));
            }
            return true;
        }
        else {
            return false;
        }
    }

    public HostControlConsole initConsole(QuorumGame game) {
        String host = getHost(game);
        String account = getAccount(game);
        String bootCmd = getBootCmd(game);
        String cachePath = getCachePath(game);

        ServiceKey<HostControlConsole> key = CloudContext.Helper.key(HostControlConsole.class);
        key = key.with("host", host).with("account", account);
        if (bootCmd != null) {
            key = key.with("java", bootCmd);
        }
        if (cachePath != null) {
            key = key.with("cache-path", cachePath);
        }

        return game.getCloudContext().lookup(key, new TunnelInitializer(key, game));
    }

    protected String getHost(QuorumGame game) {
        String host = game.get(SshSpiConf.SPI_SSH_TARGET_HOST);
        if (host == null) {
            throw new RuntimeException("Remote hostname is not resolved");
        }
        return host;
    }

    protected String getAccount(QuorumGame game) {
        String account = game.get(SshSpiConf.SPI_SSH_TARGET_ACCOUNT);
        if (account == null) {
            throw new RuntimeException("Remote account is not resolved");
        }
        return account;
    }

    protected String getBootCmd(QuorumGame game) {
        String cmd = game.get(SshSpiConf.SPI_BOOTSTRAP_JVM_EXEC);
        return cmd;
    }

    protected String resolveBootCmd(QuorumGame game) {
        String cmd = game.get(SshSpiConf.REMOTE_BOOTSTRAP_JVM_EXEC);
        if (cmd == null) {
            cmd = game.get(SshSpiConf.SPI_BOOTSTRAP_JVM_EXEC);
        }
        if (cmd == null) {
            cmd = game.get(ViConf.JVM_EXEC_CMD);
        }
        if (cmd == null) {
            cmd = game.get(SshSpiConf.REMOTE_FALLBACK_JVM_EXEC);
        }
        if (cmd == null) {
            throw new RuntimeException("No Java executable configured");
        }
        return cmd;
    }

    protected String getCachePath(QuorumGame game) {
        String path = game.get(SshSpiConf.SPI_JAR_CACHE);
        return path;
    }

    protected String resolveCachePath(QuorumGame game) {
        String path = game.get(SshSpiConf.SPI_JAR_CACHE);
        if (path == null) {
            path = game.get(SshSpiConf.REMOTE_JAR_CACHE);
            if (path != null) {
                game.setProp(SshSpiConf.SPI_JAR_CACHE, path);
            }
        }
        if (path == null) {
            throw new RuntimeException("Jar cache path is not configured");
        }
        return path;
    }

    protected String resolvePassword(QuorumGame game) {
        String password = game.get(SSH_PASSWORD);
        if (password == null) {
            password = game.get(SPI_SSH_PASSWORD);
        }
        return password;
    }

    protected String resolveKeyFile(QuorumGame game) {
        String keyFile = game.get(SSH_PRIVATE_KEY_FILE);
        if (keyFile == null) {
            keyFile = game.get(SPI_SSH_PRIVATE_KEY_FILE);
        }
        return keyFile;
    }

    private class TunnelInitializer implements ServiceProvider<HostControlConsole> {

        private ServiceKey<HostControlConsole> key;
        private QuorumGame game;
        private CloudContext cloudContext;
        private HostControlConsole console;
        private HostConsoleWrapper consoleWrapper = new HostConsoleWrapper(this);

        public TunnelInitializer(ServiceKey<HostControlConsole> key, QuorumGame game) {
            this.key = key;
            this.game = game;
        }

        @Override
        public HostControlConsole getService(CloudContext context) {
            if (console == null) {
                this.cloudContext = context;
                ensureConsole();
            }

            return consoleWrapper;
        }

        void ensureConsole() {
            if (console != null) {
                return;
            }

            CloudContext context = cloudContext;

            String host = getHost(game);
            String account = getAccount(game);
            String bootCmd = resolveBootCmd(game);
            String cachePath = resolveCachePath(game);

            SimpleSshSessionProvider factory = new SimpleSshSessionProvider();
            factory.setUser(account);

            // TODO JSch options
            for(String key: game.getConfigProps(SPI_SSH_JSCH_OPTION).keySet()) {
                String opt = key.substring(SPI_SSH_JSCH_OPTION.length());
                factory.setConfig(opt, (String)game.get(key));
            }

            String password = resolvePassword(game);
            String keyFile = resolveKeyFile(game);

            if (password != null) {
                factory.setPassword(password);
            }
            if (keyFile != null) {
                factory.setKeyFile(keyFile);
            }

            Session session;
            try {
                session = factory.getSession(host, account);
            } catch (JSchException e) {
                throw new RuntimeException(e);
            }
            SshHostControlConsole shhConsole = new SshHostControlConsole(session, cachePath, true, 1);
            // TODO logger configuration
            TunnellerInitiator initiator = new SimpleTunnelInitiator(bootCmd, cachePath, context.lookup(CloudContext.Helper.key(StreamCopyService.class)), ZLogFactory.getDefaultRootLogger());
            console = initiator.initTunnel(shhConsole);

            ServiceKey<HostControlConsole> key2 = CloudContext.Helper.key(HostControlConsole.class);
            key2 = key2.with("host", host).with("account", account);
            if (!key.equals(key2)) {
                context.lookup(key2, this);
            }
            ServiceKey<HostControlConsole> key3;
            key3 = key2.with("java", bootCmd);
            if (!key.equals(key3)) {
                context.lookup(key3, this);
            }
            key3 = key2.with("cache-path", cachePath);
            if (!key.equals(key3)) {
                context.lookup(key3, this);
            }
            key3 = key2.with("java", bootCmd).with("cache-path", cachePath);
            if (!key.equals(key3)) {
                context.lookup(key3, this);
            }

            context.addFinalizer(new KillConsole(console));
        }
    }

    private class HostConsoleWrapper implements HostControlConsole {

        TunnelInitializer initializer;

        public HostConsoleWrapper(TunnelInitializer initializer) {
            this.initializer = initializer;
        }

        @Override
        public boolean isLocalFileSystem() {
            return false;
        }

        @Override
        public String getHostname() {
            synchronized(initializer) {
                initializer.ensureConsole();
                try {
                    return initializer.console.getHostname();
                }
                catch(Exception e) {
                    initializer.console = null;
                    throw throwAny(e);
                }
            }
        }

        @Override
        public String cacheFile(FileBlob blob) {
            synchronized(initializer) {
                initializer.ensureConsole();
                try {
                    return initializer.console.cacheFile(blob);
                }
                catch(Exception e) {
                    initializer.console = null;
                    throw throwAny(e);
                }
            }
        }

        @Override
        public List<String> cacheFiles(List<? extends FileBlob> blobs) {
            synchronized(initializer) {
                initializer.ensureConsole();
                try {
                    return initializer.console.cacheFiles(blobs);
                }
                catch(Exception e) {
                    initializer.console = null;
                    throw throwAny(e);
                }
            }
        }

        @Override
        public Destroyable openSocket(SocketHandler handler) {
            synchronized(initializer) {
                initializer.ensureConsole();
                try {
                    return initializer.console.openSocket(handler);
                }
                catch(Exception e) {
                    initializer.console = null;
                    throw throwAny(e);
                }
            }
        }

        @Override
        public Destroyable startProcess(String workDir, String[] command, Map<String, String> env, ProcessHandler handler) {
            synchronized(initializer) {
                initializer.ensureConsole();
                try {
                    return initializer.console.startProcess(workDir, command, env, handler);
                }
                catch(Exception e) {
                    initializer.console = null;
                    throw throwAny(e);
                }
            }
        }

        @Override
        public void terminate() {
            synchronized(initializer) {
                if (initializer.console != null) {
                    initializer.console.terminate();
                    initializer.console = null;
                }
            }
        }

        private RuntimeException throwAny(Throwable e) {
            RemoteConsoleInitializer.<RuntimeException>doThrow(e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E doThrow(Throwable e) throws E {
        throw (E)e;
    }

    private static class KillConsole implements Runnable {

        private final HostControlConsole console;

        public KillConsole(HostControlConsole console) {
            this.console = console;
        }

        @Override
        public void run() {
            console.terminate();
        }
    }
}
