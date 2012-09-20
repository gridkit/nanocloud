package org.gridkit.vicluster.isolate.coherence;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.gridkit.vicluster.ViGroup;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.isolate.IsolateViNode;
import org.junit.After;
import org.junit.Test;

import com.tangosol.net.CacheFactory;

public class MultiVersionTest {

	private static Map<String, String> ISOLATE_PROPS = new HashMap<String, String>();
	static {
		ISOLATE_PROPS.put("isolate:package:org.gridkit", "");
		ISOLATE_PROPS.put("isolate:package:com.tangosol", "");
	}		
	
	ViGroup hosts = new ViGroup();
	
	private IsolateViNode createIsolateViHost(String name) {
		IsolateViNode viHost = new IsolateViNode(name);
		hosts.addNode(viHost);
		return viHost;
	}
	
	@After
	public void cleanIsolates() {
		hosts.shutdown();
		hosts = new ViGroup();
	}
	
	@Test
	public void test_classpath_extention() throws IOException {
		
		ViNode node1 = createIsolateViHost("node-3.7.1.3");
		ViNode node2 = createIsolateViHost("node-3.7.1.5");
		ViNode nodes = ViGroup.group(node1, node2);
		nodes.setProps(ISOLATE_PROPS);
		
		CohHelper.enableFastLocalCluster(nodes);
		
		String checkPath = CacheFactory.class.getName().replace('.', '/') + ".class";
		String cohJarPath = getClass().getClassLoader().getResource(checkPath).toString();
		cohJarPath = cohJarPath.substring(0, cohJarPath.lastIndexOf('!')) + "!/";
		
		System.out.println("Excluding: " + cohJarPath);
		
		IsolateViNode.removeFromClasspath(nodes, new URL(cohJarPath));
		
		URL jar1 = getClass().getResource("/coherence-lib/coherence-3.7.1.3.jar");
		URL path1 = new URL("zip:" + jar1.toString() + "!/");
		IsolateViNode.addToClasspath(node1, path1);

		URL jar2 = getClass().getResource("/coherence-lib/coherence-3.7.1.5.jar");
		URL path2 = new URL("zip:" + jar2.toString() + "!/");
		IsolateViNode.addToClasspath(node2, path2);
		
		new URL(path2, checkPath).getContent();
		
		nodes.exec(new Callable<Void>() {
			
			@Override
			public Void call() throws Exception {
				CacheFactory.getCache("distr-A");
				System.out.println("Version: " + CacheFactory.VERSION);
				return null;
			}
		});
	}
}
