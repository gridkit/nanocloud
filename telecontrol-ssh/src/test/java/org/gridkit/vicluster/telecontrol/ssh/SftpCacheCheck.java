package org.gridkit.vicluster.telecontrol.ssh;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.gridkit.internal.com.jcraft.jsch.JSchException;
import org.gridkit.internal.com.jcraft.jsch.Session;
import org.gridkit.internal.com.jcraft.jsch.SftpException;
import org.junit.Test;

public class SftpCacheCheck {

	public Session createSession() throws JSchException {
		SimpleSshSessionProvider sf = new SimpleSshSessionProvider();
//		sf.setUser("root");
//		sf.setPassword("toor");
//		return sf.getSession("cbox1", null);		
		sf.setUser("grimisuat");
		sf.setPassword("@Mon_day5");
		return sf.getSession("longmrdfappd1.uk.db.com", null);		
	}
	
	@Test
	public void test_simple_upload() throws JSchException, InterruptedException, SftpException {
		SftFileCache cache = new SftFileCache(createSession(), "/tmp/.vigrid/.jarcache", 1); 

		TestBlob blobA = new TestBlob("A", "AAAAAAA".getBytes()); 
		TestBlob blobB = new TestBlob("B", "BBBBBBB".getBytes()); 

		cache.upload(blobA);
		cache.upload(blobB);
		cache.upload(blobA);
		cache.upload(blobB);
		
	}

	@Test
	public void test_parallel_upload() throws JSchException, InterruptedException, SftpException {
		SftFileCache cache = new SftFileCache(createSession(), "/tmp/.vigrid/.jarcache", 4); 
		
		List<TestBlob> blobs = new ArrayList<TestBlob>();
		Random r = new Random();
		for(int i = 0; i != 100; ++i) {
			byte[] data = new byte[(128 << 10) + r.nextInt(64 << 10)];
			r.nextBytes(data);
			blobs.add(new TestBlob("file-" + i, data));
		}
		
		cache.upload(blobs);		
	}
	
	public static class TestBlob implements RemoteFileCache2.Blob {

		private String filename;
		private String hash;
		private byte[] data;
		
		public TestBlob(String filename, byte[] data) {
			this.filename = filename;
			this.data = data;
			this.hash = StreamHelper.digest(data, "SHA-1");
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
