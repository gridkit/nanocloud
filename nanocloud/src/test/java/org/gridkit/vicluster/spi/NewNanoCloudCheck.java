package org.gridkit.vicluster.spi;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Callable;

import org.gridkit.vicluster.NanoNode;
import org.gridkit.vicluster.ViCloud;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NewNanoCloudCheck {
	
	private ViCloud<NanoNode> cloud;
	
	@Before
	public void initCloud() {
		cloud = NanoCloudFactory.createCloud();
		NanoCloudConfig config = NanoCloudFactory.newConfig();

		config.forLabel("local")
			.useLocalJvmHost();

		config.forLabel("isolate")
			.useEmbededHost();
		
		config.forLabel("remote")
			.useSshRemoting();

		config.forLabel("host1")
			.useHost("longmrdfappd1.uk.db.com");

		config.forLabel("host2")
			.useHost("longmrdfappd2.uk.db.com");

		config.forLabel("host3")
			.useHost("longmrdfappd3.uk.db.com");
		
		config.forHost("longmrdfappd*.uk.db.com")
			.useAccount("coreserv");

		config.forHost("longmrdfappd*.uk.db.com")
			.usePassword("Tang0s0l1");

		config.forHost("longmrdfappd*.uk.db.com")
			.useAgentHomeAt("/tmp/.gridagent2");
		
		config.forHostGroup("group1")
			.useHosts("longmrdfappd1.uk.db.com", "longmrdfappd2.uk.db.com", "longmrdfappd2.uk.db.com");
		
		cloud.applyConfig(config);
	}
	
	@After
	public void dropCloud() {
		cloud.shutdown();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void test_unconfigure() {
		
		NanoNode node = cloud.node("node-a");
		node.ensure();
	}

	@Test
	public void test_isolate() {
		
		NanoNode node = cloud.node("node-a");
		node.label("isolate");
		
		node.exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Hallo isolate");
			}
		});
	}

	@Test
	public void test_local() {
		
		NanoNode node = cloud.node("node-a");
		node.label("local");
		
		node.exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Hallo nanocloud");
			}
		});
	}

	@Test
	public void test_remote() {
		
		NanoNode node = cloud.node("node-a");
		node.label("remote");
		node.label("host1");
		
		node.exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Hallo nanocloud");
			}
		});
	}

	@Test
	public void test_remote_parallel() {
		
		cloud.node("node-*").label("remote");

		cloud.node("node-a").label("host1");
		cloud.node("node-b").label("host2");
		cloud.node("node-c").label("host3");
		
		
		cloud.node("node-*").exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Hallo nanocloud");
			}
		});
	}

	@Test
	public void test_host_group() {
		
		cloud.node("node-*").label("remote");
		cloud.node("node-*").remote().hostGroup("group1");
		
		cloud.node("node-a").touch();
		cloud.node("node-b").touch();
		cloud.node("node-c").touch();
		cloud.node("node-d").touch();
		
		List<String> hosts = cloud.node("node-*").massExec(new Callable<String>() {
			@Override
			public String call() throws Exception {
				String host = InetAddress.getLocalHost().getHostName();
				System.err.println("Hallo: " + host);
				return host;
			}
		});
		
		System.err.println("Hosts: " + hosts);
	}
}
