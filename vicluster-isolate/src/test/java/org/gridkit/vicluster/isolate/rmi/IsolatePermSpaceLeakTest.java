package org.gridkit.vicluster.isolate.rmi;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gridkit.vicluster.ViCloud;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.isolate.IsolateViNode;
import org.gridkit.vicluster.spi.IsolateFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class IsolatePermSpaceLeakTest {

	ViCloud<IsolateViNode>  cloud;
	
	@Before 
	public void initCloud() {
		cloud = IsolateFactory.createIsolateCloud();
		cloud.byName("**").isolation().includePackage("org.gridkit");
	}
	
	@After
	public void dropCloud() {
		cloud.shutdown();
	}
	
	@Test
	public void test_permanent_leak() {

		// 100 is a bit too short, but test should not take forever
		int interations = 500;
		for(int i = 0; i != interations; ++i) {
			IsolateViNode node = cloud.node("node-" + i);
			node.isolation().includePackage("org.gridkit");
			// loading XML library to an isolate to put some stress on perm gen
			node.isolation().includePackage("org.w3c");
			node.isolation().includePackage("javax.xml");
			node.isolation().includePackage("com.sun.org.apache.xerces");
			node.isolation().includePackage("com.sun.xml");
			
			node.exec(new VoidCallable() {
				@Override
				public void call() throws Exception {
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document doc = builder.newDocument();
					Element el = doc.createElement("root");
					el.setAttribute("attr", "value");
					el.appendChild(doc.createCDATASection("HAHAH"));
					el.appendChild(doc.createElement("child"));
					el.appendChild(doc.createTextNode("text"));
					doc.appendChild(el);
					
					doc.normalizeDocument();
				}
			});
			
			node.shutdown();
			
			if (i % 100 == 99) {
				for(MemoryPoolMXBean mpool: ManagementFactory.getMemoryPoolMXBeans()) {
					if (mpool.getName().toUpperCase().contains("PERM")) {
						System.out.println(mpool.getName() + ": " + (mpool.getUsage().getUsed() >> 20) + "M");
					}
				}
			}
		}
	}	
}
