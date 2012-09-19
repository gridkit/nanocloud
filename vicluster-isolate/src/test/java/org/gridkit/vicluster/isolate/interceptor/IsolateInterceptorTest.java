package org.gridkit.vicluster.isolate.interceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.gridkit.lab.interceptor.isolate.InterceptorClassTransformer;
import org.gridkit.vicluster.ViGroup;
import org.gridkit.vicluster.VoidCallable;
import org.gridkit.vicluster.isolate.IsolateViNode;
import org.junit.After;
import org.junit.Test;

public class IsolateInterceptorTest {

	private static Map<String, String> ISOLATE_PROPS = new HashMap<String, String>();
	static {
		ISOLATE_PROPS.put("isolate:package:org.gridkit", "");
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
	public void try_btrace() throws IOException {
		
		IsolateViNode viHost1 = createIsolateViHost("node-1");
		IsolateViNode viHost2 = createIsolateViHost("node-2");
		
		viHost1.getIsolate();
		viHost2.getIsolate();
		
		ViGroup group = ViGroup.group(viHost1, viHost2);
		group.setProps(ISOLATE_PROPS);
		
		InterceptorClassTransformer ict = new InterceptorClassTransformer();
		ict.addProbe(new NanoTimeProbe(), "java/lang/System", "nanoTime", new String[0]);
		
		viHost1.getIsolate().setClassTransformer(ict);

		System.out.println("\n\n\n\n\n");
		
		System.out.println("Time: " + System.nanoTime());

		viHost1.exec(new VoidCallable() {
			@Override
			public void call() throws Exception {
				System.out.println("Time: " + System.nanoTime());
				testCall();
			}
			
			public void testCall() {
				System.out.println("test");
			}
		});

		System.out.println("\n\n\n\n\n");
		
		viHost2.exec(new VoidCallable() {
			@Override
			public void call() throws Exception {
				System.out.println("Time: " + System.nanoTime());
			}
		});		
	}
}
