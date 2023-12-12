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
package org.gridkit.vicluster;

import org.junit.Test;

import junit.framework.Assert;

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
