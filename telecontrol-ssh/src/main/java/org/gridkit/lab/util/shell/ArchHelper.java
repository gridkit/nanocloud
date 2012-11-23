package org.gridkit.lab.util.shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ArchHelper {

	public static void uncompressZip(File archive, File destination) throws IOException {
		ZipFile file = new ZipFile(archive);
		Enumeration<? extends ZipEntry> n = file.entries();
		while(n.hasMoreElements()) {
			ZipEntry entry = n.nextElement();
			File f = new File(destination, entry.getName());
			FileOutputStream fos = new FileOutputStream(f);
			copy(file.getInputStream(entry), fos);
			fos.close();
		}
	}

	public static void uncompressGz(File archive, File destination) throws IOException {
		GZIPInputStream gzin = new GZIPInputStream(new FileInputStream(archive));
		FileOutputStream fos = new FileOutputStream(destination);
		copy(gzin, fos);
		fos.close();
		gzin.close();
	}
	
	static void copy(InputStream in, OutputStream out) throws IOException {
		try {
			byte[] buf = new byte[1 << 12];
			while(true) {
				int n = in.read(buf);
				if(n >= 0) {
					out.write(buf, 0, n);
				}
				else {
					break;
				}
			}
		} finally {
			try {
				in.close();
			}
			catch(Exception e) {
				// ignore
			}
		}
	}		
}
