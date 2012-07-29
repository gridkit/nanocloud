package org.gridkit.vicluster.telecontol.ssh;

import junit.framework.Assert;

import org.gridkit.vicluster.telecontrol.ssh.SimpleSshSessionProvider;
import org.junit.AfterClass;
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
		
		Session session = sshFactory.getSession("localhost:11022");
		Assert.assertTrue(session.isConnected());
	}
	
	@Test
	public void test_ki_password_auth() throws JSchException {
		JSch.setConfig("PreferredAuthentications", "gssapi-with-mic,publickey,keyboard-interactive");
		SimpleSshSessionProvider sshFactory = new SimpleSshSessionProvider();
		sshFactory.setUser("ubuntu");
		sshFactory.setPassword("reverse");
		
		Session session = sshFactory.getSession("localhost:11022");
		Assert.assertTrue(session.isConnected());
	}

	@Test
	public void test_public_key_auth() throws JSchException {
		JSch.setConfig("PreferredAuthentications", "gssapi-with-mic,publickey");
		SimpleSshSessionProvider sshFactory = new SimpleSshSessionProvider();
		sshFactory.setUser("ubuntu");
		sshFactory.setKeyFile("C:/.ssh/aragozin.rsa");
		
		Session session = sshFactory.getSession("localhost:11022");
		Assert.assertTrue(session.isConnected());
	}

	@AfterClass
	public static void restorePerferedAuth() {
		JSch.setConfig("PreferredAuthentications", "gssapi-with-mic,publickey,keyboard-interactive,password");
	}
}
