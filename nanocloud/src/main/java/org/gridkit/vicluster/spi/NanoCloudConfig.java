package org.gridkit.vicluster.spi;

import org.gridkit.vicluster.spi.RuleBuilder.GenericRule;
import org.gridkit.vicluster.spi.RuleBuilder.GenericRuleS2;

public class NanoCloudConfig extends RuleSet {

	public NodeRule forLabel(String label) {
		GenericRuleS2 gr = RuleBuilder.newRule().condition();
		gr.label(label);
		return new NodeRuleWrapper(gr);
	}

	public NodeRule forNode(String pattern) {
		GenericRuleS2 gr = RuleBuilder.newRule().condition();
		gr.matchName(pattern);		
		return new NodeRuleWrapper(gr);
	}

	public HostRule forHost(String pattern) {
		GenericRuleS2 gr = RuleBuilder.newRule().condition();
		gr.matchName(pattern);		
		return new HostRuleWrapper(gr);
	}

	public HostRule forHostsLabeledWith(String label) {
		GenericRuleS2 gr = RuleBuilder.newRule().condition();
		gr.label(label);		
		return new HostRuleWrapper(gr);
	}
	
	public HostGroupRule forHostGroup(String pattern) {
		addRule(
			RuleBuilder.newPrototype()
				.activation()
					.name(pattern)
					.type(HostGroup.class)
				.configuration()
					.instantiator(new HostGroupInstantiator())
		);
		GenericRuleS2 gr = RuleBuilder.newRule().condition();
		gr.matchName(pattern);		
		return new HostGroupRuleWrapper(gr);		
	}

	public interface NodeRule {
		
		public void useSshRemoting();
		
		public void useEmbededHost();
		
		public void useLocalJvmHost();
		
		public void useHost(String host);

		public void useHostgroup(String group);

		public void useHostgroup(String group, String colocId);

		public void useColocation(String colocId);
		
		public void useAccount(String account);
		
	}

	public interface HostRule {
		
		public void label(String label);
		
		public void useHostname(String hostname);
		
		public void useAccount(String account);

		public void usePassword(String password);

		public void usePrivateKeyFile(String privateKey);
		
		public void useAgentHomeAt(String path);

		public void useDefaultJava(String javaCmd);
		
	}
	
	public interface HostGroupRule {
		
		public void useHosts(String... hosts);
		
	}
	
	private class NodeRuleWrapper implements NodeRule {
		
		private final GenericRuleS2 gr;

		public NodeRuleWrapper(GenericRuleS2 gr) {
			this.gr = gr;
		}

		@Override
		public void useEmbededHost() {
			addRule(
				gr
					.type(ViNodeSpi.class)
				.defaultValue()
					.instantiator(new IsolateNodeInstantiator())					
			);			
		}

		@Override
		public void useLocalJvmHost() {
			addRule(
				gr
					.type(ViNodeSpi.class)
				.configuration()
					.a(NanoNodeInstantiator.HOST, new LocalHostResolver())
			);
		}
		
		@Override
		public void useSshRemoting() {
			addRule(
				gr
					.type(ViNodeSpi.class)
				.configuration()
					.a(NanoNodeInstantiator.HOST, new RemoteHostResolver())
			);
		}

		@Override
		public void useHost(String host) {
			addRule(
				gr
					.type(HostConfiguration.class)
				.configuration()
					.a(RemoteAttrs.HOST_HOSTNAME, host)
			);			
		}

		@Override
		public void useHostgroup(String group) {
			addRule(
				gr
					.type(ViNodeSpi.class)
				.configuration()
					.a(RemoteAttrs.NODE_HOSTGROUP, group)
			);			
		}

		@Override
		public void useHostgroup(String group, String colocId) {
			addRule(
				gr
					.type(ViNodeSpi.class)
				.configuration()
					.a(RemoteAttrs.NODE_HOSTGROUP, group)
					.a(RemoteAttrs.NODE_COLOCATION_ID, colocId)
			);			
		}

		@Override
		public void useColocation(String colocId) {
			addRule(
				gr
					.type(ViNodeSpi.class)
				.configuration()
					.a(RemoteAttrs.NODE_COLOCATION_ID, colocId)
			);			
		}

		@Override
		public void useAccount(String account) {
			addRule(
				gr
					.type(HostConfiguration.class)
				.configuration()
					.a(RemoteAttrs.HOST_LOGIN, account)
			);			
		}
	}
	
	private class HostRuleWrapper implements HostRule {
		
		private final GenericRuleS2 gr;

		public HostRuleWrapper(GenericRuleS2 gr) {
			this.gr = gr;
		}

		@Override
		public void label(String label) {
			addRule(
				gr
					.type(Host.class)
				.configuration()
					.label(label)									
			);		
		}

		@Override
		public void useHostname(String hostname) {
			addRule(
				gr
					.type(Host.class)
				.defaultValue()
					.a(RemoteAttrs.HOST_HOSTNAME, hostname)					
			);		
		}

		@Override
		public void useAccount(String account) {
			addRule(
				gr
					.type(Host.class)
				.defaultValue()
					.a(RemoteAttrs.HOST_LOGIN, account)					
			);		
		}

		@Override
		public void usePassword(String password) {
			addRule(
				gr
					.type(Host.class)
				.defaultValue()
					.a(RemoteAttrs.HOST_PASSWORD, password)					
			);		
		}

		@Override
		public void usePrivateKeyFile(String privateKey) {
			addRule(
				gr
					.type(Host.class)
				.defaultValue()
					.a(RemoteAttrs.HOST_PRIVATE_KEY_PATH, privateKey)					
			);		
		}

		@Override
		public void useAgentHomeAt(String path) {
			addRule(
				gr
					.type(Host.class)
				.defaultValue()
					.a(RemoteAttrs.HOST_AGENT_HOME, path)					
			);		
		}

		@Override
		public void useDefaultJava(String javaCmd) {
			addRule(
				gr
					.type(Host.class)
				.defaultValue()
					.a(RemoteAttrs.HOST_DEFAULT_JAVA, javaCmd)					
			);		
		}
	}
	
	private class HostGroupRuleWrapper implements HostGroupRule {
		
		private final GenericRuleS2 gr;

		public HostGroupRuleWrapper(GenericRuleS2 gr) {
			this.gr = gr;
		}

		@Override
		public void useHosts(String... hosts) {
			GenericRule r = gr
				.type(HostGroup.class)
			.configuration();
			
			for(String host: hosts) {
				r.a(RemoteAttrs.HOSTGROUP_HOST, host);
			}
			
			addRule(r);			
		}
	}			
}
