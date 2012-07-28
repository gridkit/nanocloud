package org.gridkit.fabric.ssh;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Test;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class ChechSshConnection {

	@Test
	public void test_password_auth() throws JSchException {
		JSch.setConfig("PreferredAuthentications", "gssapi-with-mic,publickey,password");
		DefaultSSHFactory sshFactory = new DefaultSSHFactory();
		sshFactory.setUser("ubuntu");
		sshFactory.setPassword("reverse");
		
		Session session = sshFactory.getSession("localhost:11022");
		Assert.assertTrue(session.isConnected());
	}
	
	@Test
	public void test_ki_password_auth() throws JSchException {
		JSch.setConfig("PreferredAuthentications", "gssapi-with-mic,publickey,keyboard-interactive");
		DefaultSSHFactory sshFactory = new DefaultSSHFactory();
		sshFactory.setUser("ubuntu");
		sshFactory.setPassword("reverse");
		
		Session session = sshFactory.getSession("localhost:11022");
		Assert.assertTrue(session.isConnected());
	}

	@Test
	public void test_public_key_auth() throws JSchException {
		JSch.setConfig("PreferredAuthentications", "gssapi-with-mic,publickey");
		DefaultSSHFactory sshFactory = new DefaultSSHFactory();
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
