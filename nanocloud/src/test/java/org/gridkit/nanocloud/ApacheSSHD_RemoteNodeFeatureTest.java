package org.gridkit.nanocloud;

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
import org.junit.Test;
import org.springframework.util.SocketUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.util.Collections;

import static org.gridkit.nanocloud.RemoteNode.REMOTE;

public class ApacheSSHD_RemoteNodeFeatureTest extends ViNodeFeatureTest {

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
	
	@Before
	@Override
	public void initCloud() {
		cloud = CloudFactory.createCloud();
		cloud.node("**").x(REMOTE)
			.useSimpleRemoting()
            .setHostsConfigFile("?na")
			.setRemoteHost("localhost:"+sshServer.getPort())
            .setRemoteAccount("agent.smith")
            .setPassword("matrix")
            .setRemoteJavaExec('"'+javaExec+'"')
            .setRemoteJarCachePath("./target/cache");
	}

    @Test
    @Override
    public void verify_isolated_static_with_void_callable() {
        super.verify_isolated_static_with_callable();
    }

    @Test
    @Override
    public void verify_isolated_static_with_callable() {
        super.verify_isolated_static_with_callable();
    }

    @Test
    @Override
    public void verify_isolated_static_with_runnable() {
        super.verify_isolated_static_with_runnable();
    }

    public void verify_class_exclusion() {
        // class sharing is not supported by local nodes, obviously
    }       

    @Test
    @Override
    public void verify_property_isolation() throws Exception {
        super.verify_property_isolation();
    }
    
    @Test
    @Override
    public void verify_exec_stack_trace_locality() {
        super.verify_exec_stack_trace_locality();
    }

    @Test
    @Override
    public void verify_transparent_proxy_stack_trace() {
        super.verify_transparent_proxy_stack_trace();
    }

    @Test
    @Override
    public void verify_transitive_transparent_proxy_stack_trace() {
        super.verify_transitive_transparent_proxy_stack_trace();
    }

    @Test
    @Override
    public void test_classpath_extention() throws IOException, URISyntaxException {
        super.test_classpath_extention();
    }

    @Test
    @Override
    public void test_dont_inherit_cp() {
        super.test_dont_inherit_cp();
    }

    @Test
    @Override
    public void test_handle_NoDefClassFound(){
        super.test_handle_NoDefClassFound();
    }

    @Test
    @Override
    public void test_handle_NoDefClassFound_on_return(){
        super.test_handle_NoDefClassFound_on_return();
    }
    
    @Test
    @Override
    public void test_inherit_cp_true() throws IOException, URISyntaxException {
        super.test_inherit_cp_true();
    }

    @Test
    @Override
    public void test_inherit_cp_shallow() throws IOException, URISyntaxException {
    	super.test_inherit_cp_shallow();
    }
    
    @Test
    @Override
    public void test_inherit_cp_default_true() {
        super.test_inherit_cp_default_true();
    }

    @Test
    @Override
    public void test_classpath_limiting() throws MalformedURLException, URISyntaxException {
        super.test_classpath_limiting();
    }

    @Test
    @Override
    public void test_annonimous_primitive_in_args() {
        super.test_annonimous_primitive_in_args();
    }

    @Test
    @Override
    public void verify_new_env_variable() {
        super.verify_new_env_variable();
    }

    @Test
    @Override
    public void verify_env_variable_removal() {
        super.verify_env_variable_removal();
    }

    @Test
    @Override
    public void verify_jvm_single_arg_passing() {
        super.verify_jvm_single_arg_passing();
    }

    @Test
    @Override
    public void verify_jvm_multiple_args_passing() {
        super.verify_jvm_multiple_args_passing();
    }

    @Test
    @Override
    public void verify_jvm_invalid_arg_error() {
        super.verify_jvm_invalid_arg_error();
    }

    @Test
    @Override
    public void verify_slave_working_dir() throws IOException {
        super.verify_slave_working_dir();
    }

    @Test
    @Override
    public void verify_jvm_agent() throws Exception {
        super.verify_jvm_agent();
    }

    @Test
    @Override
    public void verify_jvm_agent_with_options() throws Exception {
        super.verify_jvm_agent_with_options();
    }

    @Test
    @Override
    public void verify_jvm_agent_multiple_agents() throws Exception {
        super.verify_jvm_agent_multiple_agents();
    }
}
