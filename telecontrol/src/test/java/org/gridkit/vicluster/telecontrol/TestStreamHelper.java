package org.gridkit.vicluster.telecontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestStreamHelper {

	public static void copy(InputStream in, OutputStream out) throws IOException {
		StreamHelper.copy(in, out);
	}
	
	public static void link(InputStream in, OutputStream out) {
		BackgroundStreamDumper.link(in, out, false);
	}

	public static void hardLink(InputStream in, OutputStream out) {
		BackgroundStreamDumper.link(in, out, true);
	}
	
}
