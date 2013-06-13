package org.gridkit.vicluster.telecontrol;

import java.io.InputStream;

public interface FileBlob {
	
	public String getFileName();
	
	/**
	 * Cache does not care about hashing algorithm.
	 * @return
	 */
	public String getContentHash();
	
	public InputStream getContent();

	public long size();
	
}
