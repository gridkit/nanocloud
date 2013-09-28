/**
 * Copyright 2013 Alexey Ragozin
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
package org.gridkit.vicluster.telecontrol;

import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViProps;
import org.junit.Test;

public class ProcessKillerTest {

	@Test
	public void verify_shutdown_after_kill() throws InterruptedException {
		ViManager cloud = CloudFactory.createCloud();
		ViProps.at(cloud.node("**")).setLocalType();
		
		cloud.node("node1");
		cloud.node("node2");
		
		cloud.node("**").exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Hi!");
			}
		});
		
		cloud.node("node1").submit(new Runnable() {
			@Override
			public void run() {
				Runtime.getRuntime().halt(0);
			}
		});
		
		Thread.sleep(1000);
		
		cloud.shutdown();
	}
}
