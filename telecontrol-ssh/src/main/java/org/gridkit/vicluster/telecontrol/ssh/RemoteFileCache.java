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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class RemoteFileCache {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(RemoteFileCache.class);
	
	private static final String DIGEST_ALGO = "SHA-1";
	private static final String CACHE_PATH = ".cache";
	private Session session;
	private String agentHome;
	private String agentHomePath;
	private ChannelSftp sftp;

	private Map<String, String> fileMapping = new HashMap<String, String>();

	public RemoteFileCache() {		
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void setAgentHome(String agentHome) {
		this.agentHome = agentHome;
	}
	
	public void init() throws JSchException, SftpException, IOException {
		sftp = (ChannelSftp) session.openChannel("sftp");
		sftp.connect();
		
		try {
			sftp.mkdir(agentHome);
		}
		catch(SftpException e) {
			// ignore;
		}
		
		try {
			sftp.cd(agentHome);
		}
		catch(SftpException e) {
			throw new SftpException(e.id, "Failed: cd " + agentHome + " (" + e.getMessage() + ")");
		}
		agentHomePath = sftp.pwd();
		sftpMkdir(sftp, CACHE_PATH);
	}
	
	public synchronized String upload(String id, byte[] data) throws SftpException {
		if (! fileMapping.containsKey(id)) {
			String digest = StreamHelper.digest(data, DIGEST_ALGO);
			sftpMkdir(sftp, CACHE_PATH + "/" + digest);
			String name = id;
			if (name.indexOf('/') > 0) {
				name = name.substring(name.lastIndexOf('/'));
			}
			if (name.indexOf('?') > 0) {
				name = name.substring(0, name.indexOf('?'));
			}
			String rname = CACHE_PATH + "/" + digest + "/" + name;
			sftpUpload(rname, data);
			
			fileMapping.put(id, agentHomePath + "/" + rname);
		}
		return fileMapping.get(id);
	}

	private void sftpUpload(String rname, byte[] data) throws SftpException {
		int tries = 2;
		while(tries > 0) {
			--tries;
			try {
				if (!exists(sftp, rname)) {
					LOGGER.info("Uploading: " + session.getHost() + ":" + agentHomePath + "/" + rname + " " + data.length + " bytes");
					sftp.put(new ByteArrayInputStream(data), rname);
				}
				else {
					LOGGER.debug("Already exists: " + session.getHost() + ":" + agentHomePath + "/" + rname + " " + data.length + " bytes");
				}
				return;
			}
			catch(SftpException e) {
				if (tries > 0) {
					LOGGER.warn("upload \"" + rname + "\" failed: " + e.toString());
				}
				else {
					throw e;
				}
			}
		}
	}

	private static void sftpMkdir(ChannelSftp sftp, String path) throws SftpException {
		if (path.lastIndexOf('/') > 0) {
			String parPath = path.substring(0, path.lastIndexOf('/'));
			sftpMkdir(sftp, parPath);
		}
		int tries = 2;
		while(tries > 0) {
			--tries;
			try {
				if (!exists(sftp, path)) {
					sftp.mkdir(path);			
				}
				return;
			}
			catch(SftpException e) {
				if (tries > 0) {
					LOGGER.warn("mkdir has failed: " + e.toString());
				}
				else {
					throw e;
				}
			}
		}
	}

	private static boolean exists(ChannelSftp sftp, String path) {
		try {
			return sftp.stat(path) != null;
		} catch (SftpException e) {
			return false;
		}
	}
}
