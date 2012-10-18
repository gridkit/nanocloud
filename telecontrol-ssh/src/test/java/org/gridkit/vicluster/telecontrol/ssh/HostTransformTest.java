package org.gridkit.vicluster.telecontrol.ssh;

import junit.framework.Assert;

import org.gridkit.vicluster.telecontrol.ssh.ConfigurableSshReplicator;
import org.junit.Test;

public class HostTransformTest {

	@Test
	public void transform_test() {		
		Assert.assertEquals("host1", ConfigurableSshReplicator.transform("~%s!([^.]+).*", "host1.reader"));
		Assert.assertEquals("host1.gridkit.org", ConfigurableSshReplicator.transform("~%s.gridkit.org!([^.]+).*", "host1"));
		Assert.assertEquals("host1", ConfigurableSshReplicator.transform("~%2$s!([^.]+).([^.]+).*", "zoo.host1"));		
		Assert.assertEquals("host01", ConfigurableSshReplicator.transform("~host%3$02d!([^.]+).(host([0-9]+)).*", "zoo.host1"));		
		Assert.assertEquals("host1", ConfigurableSshReplicator.transform("~host%3$s!([^.]+).(host([0-9]+)).*", "zoo.host01"));		

		Assert.assertEquals("host1", ConfigurableSshReplicator.transform("~host%2$s!.*(host([0-9]+)).*", "zoo.host1"));		
		Assert.assertEquals("host1", ConfigurableSshReplicator.transform("~host%2$s!.*(host([0-9]+)).*", "zoo.host1.xxx"));		
		Assert.assertEquals("host1", ConfigurableSshReplicator.transform("~host%2$s!.*(host([0-9]+)).*", "host1.xxx"));		
	}

	@Test(expected=IllegalArgumentException.class)
	public void fail_on_wrong_group() {		
		ConfigurableSshReplicator.transform("~%3$s!([^.]+).([^.]+).*", "zoo.host1");		
	}

	@Test(expected=IllegalArgumentException.class)
	public void fail_on_wrong_pattern() {		
		ConfigurableSshReplicator.transform("~%1s!xxx.([^.]+).*", "host1");		
	}
	
}
