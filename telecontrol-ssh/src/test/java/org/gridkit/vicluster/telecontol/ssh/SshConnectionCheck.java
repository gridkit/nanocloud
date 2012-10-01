package org.gridkit.vicluster.telecontol.ssh;

import junit.framework.Assert;

import org.gridkit.vicluster.telecontrol.ssh.ConfigurableSshSessionProvider;
import org.gridkit.vicluster.telecontrol.ssh.SimpleSshSessionProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


public class SshConnectionCheck {

	@Test
	public void test_password_auth() throws JSchException {
		JSch.setConfig("PreferredAuthentications", "gssapi-with-mic,publickey,password");
		SimpleSshSessionProvider sshFactory = new SimpleSshSessionProvider();
		sshFactory.setUser("ubuntu");
		sshFactory.setPassword("reverse");
		
		Session session = sshFactory.getSession("localhost:11022", null);
		Assert.assertTrue(session.isConnected());
	}
	
	@Test
	public void test_ki_password_auth() throws JSchException {
		JSch.setConfig("PreferredAuthentications", "gssapi-with-mic,publickey,keyboard-interactive");
		SimpleSshSessionProvider sshFactory = new SimpleSshSessionProvider();
		sshFactory.setUser("ubuntu");
		sshFactory.setPassword("reverse");
		
		Session session = sshFactory.getSession("localhost:11022", null);
		Assert.assertTrue(session.isConnected());
	}

	@Test
	public void test_public_key_auth() throws JSchException {
		JSch.setConfig("PreferredAuthentications", "gssapi-with-mic,publickey");
		SimpleSshSessionProvider sshFactory = new SimpleSshSessionProvider();
		sshFactory.setUser("ubuntu");
		sshFactory.setKeyFile("C:/.ssh/aragozin.rsa");
		
		Session session = sshFactory.getSession("localhost:11022", null);
		Assert.assertTrue(session.isConnected());
	}
	
	@Test 
	public void test_configurable_factory() throws JSchException {
		ConfigurableSshSessionProvider provider = new ConfigurableSshSessionProvider();
		
		provider.hosts("*")
			.profile("password")
			.useLogin("ubuntu")
			.usePassword("reverse");
		provider.hosts("*")
			.profile("private-key")
			.useLogin("ubuntu")
			.usePrivateKey("C:/.ssh/aragozin.rsa");
		provider.hosts("*")
			.profile("ubuntu")
			.usePassword("reverse")
			.defaultProfile();
		
		Session session;
		
		session = provider.getSession("localhost:11022", null);
		Assert.assertTrue(session.isConnected());

		session = provider.getSession("localhost:11022", "password");
		Assert.assertTrue(session.isConnected());

		session = provider.getSession("localhost:11022", "private-key");
		Assert.assertTrue(session.isConnected());
		
	}

	@Before @After
	public void restorePerferedAuth() {
		JSch.setConfig("PreferredAuthentications", "gssapi-with-mic,publickey,keyboard-interactive,password");
	}
}
