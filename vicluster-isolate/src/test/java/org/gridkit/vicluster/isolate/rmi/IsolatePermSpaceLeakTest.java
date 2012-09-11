package org.gridkit.vicluster.isolate.rmi;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.isolate.IsolateViNode;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class IsolatePermSpaceLeakTest {

	@Test
	public void test_permanent_leak() {

		// 100 is a bit too short, but test should not take forever
		int interations = 500;
		for(int i = 0; i != interations; ++i) {
			ViNode node = new IsolateViNode("node-" + i);
			IsolateViNode.includePackage(node, "org.gridkit");
			// loading XML library to an isolate to put some stress on perm gen
			IsolateViNode.includePackage(node, "org.w3c");
			IsolateViNode.includePackage(node, "javax.xml");
			IsolateViNode.includePackage(node, "com.sun.org.apache.xerces");
			IsolateViNode.includePackage(node, "com.sun.xml");
			
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
