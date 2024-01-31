package org.gridkit.nanocloud;

import static org.gridkit.nanocloud.telecontrol.ssh.SshConf.SSH;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Collections;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.AcceptAllPasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.InteractiveProcessShellFactory;
import org.apache.sshd.server.shell.ProcessShellCommandFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.util.SocketUtils;

public class ApacheSSHD_Engine2_RemoteNodeFeatureTest extends ViNodeFeatureTest {

    private static SshServer sshServer;
    private static final String javaExec = getJavaExec();

    @BeforeClass
    public static void setupSshd() throws IOException {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(SocketUtils.findAvailableTcpPort());
        sshServer.setPasswordAuthenticator(AcceptAllPasswordAuthenticator.INSTANCE);
        sshServer.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            @Override
            public boolean authenticate(String username, PublicKey key, ServerSession session) throws AsyncAuthException {
                return true;
            }
        });
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshServer.setShellFactory(new InteractiveProcessShellFactory());
        sshServer.setSubsystemFactories(Collections.<NamedFactory<Command>>singletonList(
                new SftpSubsystemFactory()
        ));
        sshServer.setCommandFactory(new ProcessShellCommandFactory());
        sshServer.start();
    }

    private static String getJavaExec(){
        String javaHome = System.getProperty("java.home");
        File f = new File(javaHome);
        f = new File(f, "bin");
        f = new File(f, "java.exe");
        return f.getAbsolutePath();
    }

    @AfterClass
    public static void stopSshd() throws IOException {
        sshServer.stop(/*immediately*/true);
    }

    @SuppressWarnings("deprecation")
    @Before
    @Override
    public void initCloud() {
        cloud = Nanocloud.createCloud();
        cloud.x(SSH)
            .setSshConfigFile("?na")
            .setRemoteUrl("ssh://agent.smith@localhost:" + sshServer.getPort())
            .setPassword("matrix")
            .setRemoteJavaExec('"'+javaExec+'"')
            .setRemoteJarCachePath("./target/cache");
    }
}
