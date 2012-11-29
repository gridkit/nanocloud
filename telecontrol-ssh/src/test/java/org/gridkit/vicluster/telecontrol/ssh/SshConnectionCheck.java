/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.vicluster.telecontrol.ssh;

import junit.framework.Assert;

import org.gridkit.internal.com.jcraft.jsch.JSch;
import org.gridkit.internal.com.jcraft.jsch.JSchException;
import org.gridkit.internal.com.jcraft.jsch.Session;
import org.gridkit.vicluster.telecontrol.ssh.ConfigurableSshSessionProvider;
import org.gridkit.vicluster.telecontrol.ssh.SimpleSshSessionProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


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
