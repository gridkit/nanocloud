package org.gridkit.vicluster.telecontrol.ssh;

import java.net.InetAddress;
import java.util.concurrent.Callable;

import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.vicluster.ViManager;
import org.junit.Test;

public class SimpleCloudCheck {

	@Test
	public void verify_cbox_cluster() {
		ViManager cloud = CloudFactory.createSimpleSshCloud();
		cloud.node("cbox1");
		
		cloud.node("**").exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Hi!");
				
				ViManager rcloud = CloudFactory.createSimpleSshCloud();
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
		ViManager cloud = CloudFactory.createSimpleSshCloud();
		cloud.node("cbox1");
		
		cloud.node("**").exec(new Runnable() {
			@Override
			public void run() {
				System.out.println("Hi!");
				
				ViManager rcloud = CloudFactory.createSimpleSshCloud();
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
	ViManager cloud = CloudFactory.createSimpleSshCloud();
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
