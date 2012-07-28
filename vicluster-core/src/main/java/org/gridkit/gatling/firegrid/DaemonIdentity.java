package org.gridkit.gatling.firegrid;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SuppressWarnings("serial")
public class DaemonIdentity implements Serializable {

	private static final String LOCAL_HOST;
	private static final String LOCAL_JVM;
	static {
		try {
			LOCAL_HOST = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		LOCAL_JVM = ManagementFactory.getRuntimeMXBean().getName();
	}
	
	public static final DaemonIdentity LOCAL = new DaemonIdentity(LOCAL_HOST, LOCAL_JVM);
	
	private String host;
	private String jvmName;
	
	public DaemonIdentity(String host, String jvmName) {
		this.host = host;
		this.jvmName = jvmName;
	}
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getJvmName() {
		return jvmName;
	}



	public void setJvmName(String jvmName) {
		this.jvmName = jvmName;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((jvmName == null) ? 0 : jvmName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DaemonIdentity other = (DaemonIdentity) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (jvmName == null) {
			if (other.jvmName != null)
				return false;
		} else if (!jvmName.equals(other.jvmName))
			return false;
		return true;
	}
	
	public String toString() {
		return jvmName + "@(" + host + ")";
	}	
}
