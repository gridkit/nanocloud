package org.gridkit.vicluster;

import junit.framework.Assert;

import org.junit.Test;

public class WildPropsTest {

	@Test
	public void test_non_pattern() {
		
		WildProps props = new WildProps();
		
		props.put("A", "a");
		props.put("B", "b");
		
		Assert.assertEquals("a", props.get("A"));
		Assert.assertEquals("b", props.get("B"));
		
		props.put("A", "AAA");

		Assert.assertEquals("AAA", props.get("A"));		
	}

	@Test
	public void test_pattern() {
		
		WildProps props = new WildProps();
		
		props.put("A", "a");
		props.put("B", "b");
		props.put("*", "xxx");
		
		Assert.assertEquals("a", props.get("A"));
		Assert.assertEquals("b", props.get("B"));
		Assert.assertEquals("xxx", props.get("C"));
		
		props.put("C", "ccc");
		
		Assert.assertEquals("ccc", props.get("C"));		
	}
	
}
