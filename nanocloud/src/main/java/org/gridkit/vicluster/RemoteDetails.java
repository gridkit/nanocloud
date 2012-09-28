package org.gridkit.vicluster;

public interface RemoteDetails {

	public void host(String host);
	
	public void hostGroup(String hostgroup);

	public void hostGroup(String hostgroup, String colocId);
	
	public void colocId(String id);
	
}
