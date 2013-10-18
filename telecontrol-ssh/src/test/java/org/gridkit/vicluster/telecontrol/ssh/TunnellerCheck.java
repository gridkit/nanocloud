/**
 * Copyright 2012 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.vicluster.telecontrol.ssh;

import java.util.HashMap;
import java.util.Map;

import org.gridkit.vicluster.telecontrol.JvmConfig;
import org.gridkit.vicluster.telecontrol.ManagedProcess;
import org.junit.Test;

public class TunnellerCheck {

	public Map<String, String> config() {
		Map<String, String> config = new HashMap<String, String>();
		config.put(RemoteNodeProps.HOST, "cbox1");
		config.put(RemoteNodeProps.ACCOUNT, "root");
		config.put(RemoteNodeProps.PASSWORD, "reverse");
		config.put(RemoteNodeProps.JAVA_EXEC, "java");
		config.put(RemoteNodeProps.JAR_CACHE_PATH, "/tmp/.vigrid/.cache");
		return config;
	}
	
	@Test
	public void test_init() throws Exception {
		
		TunnellerJvmReplicator per = new TunnellerJvmReplicator();
		per.configure(config());
		per.init();
		
		ManagedProcess cp = per.createProcess("test", new JvmConfig());
		cp.bindStdOut(System.out);
		cp.bindStdOut(System.err);
		cp.getExecutionService().submit(new Runnable() {
			
			@Override
			public void run() {
				System.out.println("This is out");
				System.err.println("This is err");				
			}
		});
		
		Thread.sleep(500);
	}
	
}
