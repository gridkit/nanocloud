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
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.junit.Test;

public class SftpCacheCheck {

	public Session createSession() throws JSchException {
		SimpleSshSessionProvider sf = new SimpleSshSessionProvider();
		sf.setUser("root");
		sf.setPassword("toor");
		return sf.getSession("cbox1", null);		
	}
	
	@Test
	public void test_simple_upload() throws JSchException, InterruptedException, SftpException {
		SftFileCache cache = new SftFileCache(createSession(), "/tmp/.vigrid/.jarcache", false, 1); 

		TestBlob blobA = new TestBlob("A", "AAAAAAA".getBytes()); 
		TestBlob blobB = new TestBlob("B", "BBBBBBB".getBytes()); 

		cache.upload(blobA);
		cache.upload(blobB);
		cache.upload(blobA);
		cache.upload(blobB);
		
	}

	@Test
	public void test_parallel_upload() throws JSchException, InterruptedException, SftpException {
		SftFileCache cache = new SftFileCache(createSession(), "/tmp/.vigrid/.jarcache", false, 4); 
		
		List<TestBlob> blobs = new ArrayList<TestBlob>();
		Random r = new Random();
		for(int i = 0; i != 100; ++i) {
			byte[] data = new byte[(128 << 10) + r.nextInt(64 << 10)];
			r.nextBytes(data);
			blobs.add(new TestBlob("file-" + i, data));
		}
		
		cache.upload(blobs);		
	}
	
	public static class TestBlob implements FileBlob {

		private String filename;
		private String hash;
		private byte[] data;
		
		public TestBlob(String filename, byte[] data) {
			this.filename = filename;
			this.data = data;
			this.hash = StreamHelper.digest(data, "SHA-1");
		}

		@Override
		public File getLocalFile() {
			return null;
		}

		@Override
		public String getFileName() {
			return filename;
		}

		@Override
		public String getContentHash() {
			return hash;
		}

		@Override
		public InputStream getContent() {
			return new ByteArrayInputStream(data);
		}

		@Override
		public long size() {
			return data.length;
		}
	}
}
