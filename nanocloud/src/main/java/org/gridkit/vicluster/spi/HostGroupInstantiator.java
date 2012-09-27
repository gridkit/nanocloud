package org.gridkit.vicluster.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HostGroupInstantiator implements SpiFactory {

	@Override
	public Object instantiate(ViCloudContext context, String attrName, AttrBag config) {
		List<String> hosts = config.getAllInOrder(RemoteAttrs.HOSTGROUP_HOST);
		if (hosts.isEmpty()) {
			throw new IllegalArgumentException("Host list is empty. Bean: " + config);
		}
		SimpleHostGroup group = new SimpleHostGroup(context, hosts);
		return group;
	}

	private static class SimpleHostGroup implements HostGroup {

		private ViCloudContext context;
		
		private List<String> hosts;
		private List<String> blackList = new ArrayList<String>();
		private Map<String, String> colocation = new HashMap<String, String>();
		private int nextUncolocated = 0;
		private int nextCollocated = 0;
		
		public SimpleHostGroup(ViCloudContext context, List<String> hosts) {
			this.context = context;
			this.hosts = new ArrayList<String>(hosts);
		}

		@Override
		public synchronized Host resolveHost(AttrBag nodeConfig) {
			String coloc = nodeConfig.getLast(RemoteHostResolver.COLOCATION_ID);
			if (hosts.isEmpty()) {
				throw new IllegalArgumentException("All hosts are down");
			}
			
			if (coloc == null) {
				return nextUncolocatedHost();
			}
			else {
				return getColocatedHost(coloc);
			}
		}

		private Host nextUncolocatedHost() {
			while(true) {
				if (nextUncolocated >= hosts.size()) {
					nextUncolocated = 0;
				}
				if (hosts.isEmpty()) {
					throw new IllegalArgumentException("All hosts are down");
				}
				String hostId = hosts.get(nextUncolocated);
				Host host = getHost(hostId);
				if (host.verify()) {
					++nextUncolocated;
					return host;
				}
				else {
					hosts.remove(nextUncolocated);
					blackList.add(hostId);
					continue;
				}
			}
		}

		public Host getColocatedHost(String coloc) {
			if (colocation.containsKey(coloc)) {
				String hostId = colocation.get(coloc);
				return getHost(hostId);
			}
			while(true) {
				if (nextUncolocated >= hosts.size()) {
					throw new RuntimeException("No more hosts. Cannot find host for colocation: " + coloc); 
				}
				if (hosts.isEmpty()) {
					throw new IllegalArgumentException("All hosts are down");
				}
				String hostId = hosts.get(nextCollocated);
				Host host = getHost(hostId);
				if (host.verify()) {
					colocation.put(coloc, hostId);
					++nextCollocated;
					return host;
				}
				else {
					hosts.remove(nextCollocated);
					blackList.add(hostId);
					continue;
				}
			}
		}
		
		private Host getHost(String hostId) {
			Host host = context.getNamedInstance(hostId, Host.class);
			if (host == null) {
				throw new IllegalArgumentException("Unknown host: " + hostId);
			}
			return host;
		}
	}

}
