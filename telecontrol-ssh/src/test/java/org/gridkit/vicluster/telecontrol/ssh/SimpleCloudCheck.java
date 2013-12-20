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
package org.gridkit.vicluster.telecontrol.ssh;

import java.net.InetAddress;
import java.util.concurrent.Callable;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.SimpleCloudFactory;
import org.junit.Test;

public class SimpleCloudCheck {

	@Test
	public void verify_cbox_cluster() {
		Cloud cloud = SimpleCloudFactory.createSimpleSshCloud();
		cloud.node("cbox1");
		
		cloud.node("**").exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Hi!");
				
				Cloud rcloud = SimpleCloudFactory.createSimpleSshCloud();
				rcloud.node("localhost").exec(new Runnable() {
					@Override
					public void run() {
						System.out.println("Hi!");
					}
				});
			}
		});
	}

	@Test
	public void verify_cbox_cluster2() throws InterruptedException {
		Cloud cloud = SimpleCloudFactory.createSimpleSshCloud();
		cloud.node("cbox1");
		
		cloud.node("**").exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Hi!");
				
				Cloud rcloud = SimpleCloudFactory.createSimpleSshCloud();
				rcloud.node("cbox3").exec(new Runnable() {
					@Override
					public void run() {
						System.out.println("Hi!");
					}
				});
			}
		});
		
		Thread.sleep(2000);
	}
	
@Test
public void remote_hallo_world() throws InterruptedException {
	Cloud cloud = SimpleCloudFactory.createSimpleSshCloud();
	cloud.node("cbox1");
	
	cloud.node("**").exec(new Callable<Void>() {
		@Override
		public Void call() throws Exception {
			String localHost = InetAddress.getLocalHost().toString();
			System.out.println("Hi! I'm running on " + localHost);
			return null;
		}
	});

	// Console output is transfered asynchronously, 
	// so it is better to wait few seconds for it to arrive  
	Thread.sleep(1000);
}
	
}
