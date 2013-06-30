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
package org.gridkit.vicluster;

import java.io.Serializable;
import java.util.concurrent.Callable;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;

public class ViManagerTest {

	private ViManager man = new ViManager(new InProcessViNodeProvider());
	
	@After
	public void shutdown() {
		man.shutdown();
	}
	
	@Test
	public void select_multiple() {
		man.nodes("aa", "ab", "bb", "cc");
		
		// old dumb behavior is restored for node selectors
		
		Assert.assertEquals(2, man.nodes("aa", "*a").massExec(new Echo()).size());
		Assert.assertEquals(3, man.nodes("aa", "a*").massExec(new Echo()).size());
		Assert.assertEquals(3, man.nodes("a*", "b*").massExec(new Echo()).size());
		Assert.assertEquals(4, man.nodes("a*", "*b").massExec(new Echo()).size());
//		Assert.assertEquals(2, man.nodes("b*", "c*", "d*").massExec(new Echo()).size());
	}

	@Test(expected = IllegalStateException.class)
	public void fail_on_empty_pattern() {
		man.nodes("aa", "ab", "bb", "cc");
		
		man.node("d*").exec(new Echo());
	}
	
	@SuppressWarnings("serial")
	public static class Echo implements Callable<String>, Serializable {
		
		private String echo;

		public Echo() {
			this("echo");			
		}
		
		public Echo(String echo) {
			this.echo = echo;
		}

		@Override
		public String call() throws Exception {
			return echo;
		}
	}
}
