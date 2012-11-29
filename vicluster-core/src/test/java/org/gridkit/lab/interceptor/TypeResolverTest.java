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
package org.gridkit.lab.interceptor;

import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.gridkit.bjtest.BetterParameterized;
import org.gridkit.bjtest.BetterParameterized.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BetterParameterized.class)
public class TypeResolverTest {

	@Parameters
	public static List<?> testData() {
		Object[][] data = new Object[][]{
			{ void.class, null},
			{ boolean.class, null },
			{ byte.class, null },
			{ short.class, null },
			{ char.class, null },
			{ int.class, null },
			{ long.class, null },
			{ double.class, null },
			{ boolean[].class, null },
			{ byte[].class, null },
			{ short[].class, null },
			{ char[].class, null },
			{ int[].class, null },
			{ long[].class, null },
			{ double[].class, null },
			{ boolean[][].class, null },
			{ byte[][].class, null },
			{ short[][].class, null },
			{ char[][].class, null },
			{ int[][].class, null },
			{ long[][].class, null },
			{ double[][].class, null },
			{ String.class, null },
			{ String.class, "java/lang/String" },
			{ String[].class, null},
			{ String[][].class, null},
		};
		return Arrays.asList(data);
	}
	
	Class<?> type;
	String name;
	
	public TypeResolverTest(Class<?> type, String name) {
		this.type = type;
		this.name = name;
	}

	@Test
	@SuppressWarnings("deprecation")
	public void verify_type_resolution() throws ClassNotFoundException {
		String typeName = this.name != null ? this.name : type.getName();
		Assert.assertSame(type, ReflectionMethodCallSiteHookContext.classforName(null, typeName));
	}
	
}
