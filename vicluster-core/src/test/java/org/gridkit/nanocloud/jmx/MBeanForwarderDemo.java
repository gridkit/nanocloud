package org.gridkit.nanocloud.jmx;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;

import org.gridkit.nanocloud.jmx.MBeanRegistrator.MBeanDomainSwitcher;
import org.gridkit.nanocloud.jmx.MBeanRegistrator.MBeanServerRegistrator;
import org.junit.Test;

public class MBeanForwarderDemo {

	@Test
	public void static_test() throws MalformedObjectNameException, InterruptedException {
		
		MBeanServer mserver = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = new ObjectName("java.lang:*");
		MBeanDomainSwitcher renamer = new MBeanDomainSwitcher(new MBeanServerRegistrator(mserver), "shadow.java.lang");
		
		MBeanForwarder forwared = new MBeanForwarder(mserver, name, renamer, Executors.newSingleThreadExecutor());
		System.out.println("Started");
		
		Thread.sleep(60000);
		
		System.out.println("Stopping");
		forwared.close();
		
		Thread.sleep(60000);
	}

	@Test
	public void dynamic_static_test() throws MalformedObjectNameException, InterruptedException {
		
		MBeanServer mserver = ManagementFactory.getPlatformMBeanServer();
		ObjectName name1 = new ObjectName("java.lang:*");
		ObjectName name2 = new ObjectName("copy.*:*");
		ObjectName name3 = new ObjectName("java.util.logging:*");
		QueryExp filter = Query.or(name1, name2);
		MBeanDomainSwitcher renamer = new MBeanDomainSwitcher(new MBeanServerRegistrator(mserver), "shadow");
		MBeanDomainSwitcher renamer2 = new MBeanDomainSwitcher(new MBeanServerRegistrator(mserver), "copy.java.util.logging");
		
		MBeanForwarder forwared = new MBeanForwarder(mserver, filter, renamer, Executors.newSingleThreadExecutor());
		System.out.println("Started");
		
		Thread.sleep(60000);
		System.out.println("Add second copy");
		MBeanForwarder forwarder2 = new MBeanForwarder(mserver, name3, renamer2, Executors.newSingleThreadExecutor());

		Thread.sleep(60000);
		
		System.out.println("Stopping");
		forwarder2.close();
		Thread.sleep(60000);

		forwared.close();
		Thread.sleep(60000);
		
	}
	
}
