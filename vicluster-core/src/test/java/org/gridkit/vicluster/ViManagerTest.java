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
