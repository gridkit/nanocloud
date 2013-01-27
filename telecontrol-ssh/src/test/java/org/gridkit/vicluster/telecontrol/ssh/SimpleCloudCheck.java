package org.gridkit.vicluster.telecontrol.ssh;

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
	
}
