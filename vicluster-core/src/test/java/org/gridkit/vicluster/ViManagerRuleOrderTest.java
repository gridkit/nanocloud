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

import junit.framework.Assert;

import org.gridkit.vicluster.isolate.IsolateViNodeProvider;
import org.junit.After;
import org.junit.Test;

public class ViManagerRuleOrderTest {

	ViNodeSet cloud = new ViManager(new IsolateViNodeProvider());

	@After
	public void shutdownCloud() {
		cloud.shutdown();
	}
	
	@Test
	public void verify_init_order_specific() {
		
		ViNodeSet cloud = new ViManager(new IsolateViNodeProvider());
		
		ViNode node = cloud.node("node");
		node.addStartupHook("A", initProp("test-prop", "A"), false);
		node.addStartupHook("B", initProp("test-prop", "B"), false);
		node.addStartupHook("C", initProp("test-prop", "C"), false);
		
		node.touch();
		
		Assert.assertEquals("ABC", node.getProp("test-prop"));
	}

	@Test
	public void verify_init_order_rule() {
		
		ViNodeSet cloud = new ViManager(new IsolateViNodeProvider());
		
		ViNode node = cloud.node("node*");
		node.addStartupHook("A", initProp("test-prop", "A"), false);
		node.addStartupHook("B", initProp("test-prop", "B"), false);
		node.addStartupHook("C", initProp("test-prop", "C"), false);
		
		cloud.node("node").touch();
		
		Assert.assertEquals("ABC", cloud.node("node").getProp("test-prop"));
	}

	@Test
	public void verify_init_order_mixed_rules() {
		
		ViNodeSet cloud = new ViManager(new IsolateViNodeProvider());
		
		ViNode r1 = cloud.node("n*");
		ViNode r2 = cloud.node("*o*");
		ViNode r3 = cloud.node("*d*");
		ViNode r4 = cloud.node("*e");
		
		r4.addStartupHook("A", initProp("test-prop", "A"), false);
		r3.addStartupHook("B", initProp("test-prop", "B"), false);
		r2.addStartupHook("C", initProp("test-prop", "C"), false);
		r1.addStartupHook("D", initProp("test-prop", "D"), false);
		r2.addStartupHook("E", initProp("test-prop", "E"), false);
		r3.addStartupHook("F", initProp("test-prop", "F"), false);
		
		cloud.node("node").touch();
		
		Assert.assertEquals("ABCDEF", cloud.node("node").getProp("test-prop"));
	}

	@Test
	public void verify_init_order_mixed_rules_with_pre_declared_node() {
		
		ViNodeSet cloud = new ViManager(new IsolateViNodeProvider());

		cloud.node("node");
		
		ViNode r1 = cloud.node("n*");
		ViNode r2 = cloud.node("*o*");
		ViNode r3 = cloud.node("*d*");
		ViNode r4 = cloud.node("*e");
		
		r4.addStartupHook("A", initProp("test-prop", "A"), false);
		r3.addStartupHook("B", initProp("test-prop", "B"), false);
		r2.addStartupHook("C", initProp("test-prop", "C"), false);
		r1.addStartupHook("D", initProp("test-prop", "D"), false);
		r2.addStartupHook("E", initProp("test-prop", "E"), false);
		r3.addStartupHook("F", initProp("test-prop", "F"), false);
		
		cloud.node("node").touch();
		
		Assert.assertEquals("ABCDEF", cloud.node("node").getProp("test-prop"));
	}

	@Test
	public void verify_init_prop_override_order_with_lazy_node() {
		
		ViNodeSet cloud = new ViManager(new IsolateViNodeProvider());
		
		ViNode r1 = cloud.node("n*");
		ViNode r2 = cloud.node("*o*");
		ViNode r3 = cloud.node("*d*");
		ViNode r4 = cloud.node("*e");
		
		r4.setProp("test-prop", "r4");
		r3.setProp("test-prop", "r3");
		r2.setProp("test-prop", "r2");
		r1.setProp("test-prop", "r1");
		r2.setProp("test-prop", "r2");
		
		cloud.node("node").touch();
		
		Assert.assertEquals("r2", cloud.node("node").getProp("test-prop"));
	}

	@Test
	public void verify_init_prop_override_order_with_eager_node() {
		
		ViNodeSet cloud = new ViManager(new IsolateViNodeProvider());

		cloud.node("node");
		
		ViNode r1 = cloud.node("n*");
		ViNode r2 = cloud.node("*o*");
		ViNode r3 = cloud.node("*d*");
		ViNode r4 = cloud.node("*e");
		
		r4.setProp("test-prop", "r4");
		r3.setProp("test-prop", "r3");
		r2.setProp("test-prop", "r2");
		r1.setProp("test-prop", "r1");
		r2.setProp("test-prop", "r2");
		
		cloud.node("node").touch();
		
		Assert.assertEquals("r2", cloud.node("node").getProp("test-prop"));
	}
	
	
	private PropInitializer initProp(String name, String val) {
		return new PropInitializer(name, val);
	}
	
	@SuppressWarnings("serial")
	private static class PropInitializer implements Runnable, Serializable {

		private final String propName;
		private final String value;
		
		public PropInitializer(String propName, String value) {
			this.propName = propName;
			this.value = value;
		}

		@Override
		public void run() {
			String actual = System.getProperty(propName, "");
			actual += value;
			System.setProperty(propName, actual);
		}
	}
}
