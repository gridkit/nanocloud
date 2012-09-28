package org.gridkit.zerormi;

import java.io.EOFException;
import java.net.SocketException;


class IOHelper {
	
	public static boolean isSocketTerminationException(Exception e) {
		if (e instanceof EOFException) {
			return true;
		}
		else if (e instanceof java.io.IOException) {
			if ("pipe closed".equals(e.getMessage().toLowerCase())) {
				return true;
			}
		}
		else if (e instanceof SocketException) {
			if ("connection reset".equals(e.getMessage().toLowerCase())) {
				return true;
			}
			if ("socket closed".equals(e.getMessage().toLowerCase())) {
				return true;
			}
		}
		
		// otherwise
		return false;
	}

}
