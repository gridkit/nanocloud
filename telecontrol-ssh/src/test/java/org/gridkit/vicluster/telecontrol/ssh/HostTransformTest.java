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
		Assert.assertEquals("server.acme.com", ConfigurableSshReplicator.transform("~%s!(.*)", "server.acme.com"));		
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
