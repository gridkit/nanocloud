package org.gridkit.vicluster.telecontrol.ssh;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.gridkit.vicluster.telecontrol.ControlledProcess;
import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.ssh.ClasspathReplicator.ClasspathEntry;

public class TunnellerJvmReplicator implements RemoteJmvReplicator {

	@Override
	public void configure(Map<String, String> nodeConfig) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getFingerPrint() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public ControlledProcess createProcess(String caption, JvmConfig jvmArgs) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
//	protected Process execute(List<ClasspathEntry> classPath) {
//		
//	}
	
	@Override
	public void dispose() {
	}
}
