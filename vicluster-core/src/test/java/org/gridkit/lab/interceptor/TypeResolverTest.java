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
