package org.gridkit.vicluster.telecontrol.ssh;

import java.io.InputStream;
import java.util.List;

public interface RemoteFileCache2 {

	public String upload(Blob blob);
	
	public List<String> upload(List<? extends Blob> blobs);
	
	public static interface Blob {
		
		public String getFileName();
		
		/**
		 * Cache does not care about hashing algorithm.
		 * @return
		 */
		public String getContentHash();
		
		public InputStream getContent();

		public long size();
		
	}
}
