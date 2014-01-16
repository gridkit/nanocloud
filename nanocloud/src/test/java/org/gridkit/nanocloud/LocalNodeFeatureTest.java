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
package org.gridkit.nanocloud;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.gridkit.vicluster.VX;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class LocalNodeFeatureTest extends BasicNodeFeatureTest {

	@Before
	public void initCloud() {
		cloud = CloudFactory.createCloud();
		cloud.node("**").x(VX.TYPE).setLocal();
	}	
	
	@Test
	@Override
	public void verify_isolated_static_with_void_callable() {
		super.verify_isolated_static_with_callable();
	}

	@Test
	@Override
	public void verify_isolated_static_with_callable() {
		super.verify_isolated_static_with_callable();
	}

	@Test
	@Override
	public void verify_isolated_static_with_runnable() {
		super.verify_isolated_static_with_runnable();
	}

	@Ignore
	public void verify_class_exclusion() {
		// class sharing is not supported by local nodes, obviously
	}		

	@Test
	@Override
	public void verify_property_isolation() throws Exception {
		super.verify_property_isolation();
	}
	
	@Ignore("Not working at the moment due to limitation of dynamic proxies")
	public void verify_exec_stack_trace_locality() {
	}	

	// TODO expose export feature
	@Ignore("Feature is missing")
	public void test_stack_trace2() {
	}

	@Test
	@Override
	public void test_classpath_extention() throws IOException, URISyntaxException {
		super.test_classpath_extention();
	}

	@Test(expected = NoClassDefFoundError.class)
	@Override
	public void test_classpath_limiting() throws MalformedURLException, URISyntaxException {
		super.test_classpath_limiting();
	}

	@Test
	@Override
	public void test_annonimous_primitive_in_args() {
		super.test_annonimous_primitive_in_args();
	}
}
