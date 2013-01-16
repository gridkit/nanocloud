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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.gridkit.bjtest.BetterParameterized;
import org.gridkit.bjtest.BetterParameterized.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BetterParameterized.class)
public class GlobHelperTest {

	@Parameters
	public static Collection<Object[]> getCases() {
		List<Object[]> cases = new ArrayList<Object[]>();
		cases.add(new Object[]{"*", "x", true});
		cases.add(new Object[]{"**", "x", true});
		cases.add(new Object[]{"*", "x/y", false});
		cases.add(new Object[]{"*", "x\\y", false});
		cases.add(new Object[]{"**", "x/y", true});
		cases.add(new Object[]{"**", "x\\y", true});
		cases.add(new Object[]{"x/*/z", "x/y/z", true});
		cases.add(new Object[]{"*/x/*/z", "x/y/z", false});
		cases.add(new Object[]{"x/*/z/*", "x/y/z", false});
		cases.add(new Object[]{"x/**/z", "x/y/z", true});
		cases.add(new Object[]{"x/*/z", "x/y1/y2/z", false});
		cases.add(new Object[]{"x/**/z", "x/y1/y2/z", true});
		cases.add(new Object[]{"**/x/**/z", "x/y1/y2/z", true});
		cases.add(new Object[]{"**/x/**/z", "1/x/y1/y2/z", true});
		cases.add(new Object[]{"**/x/**/z", "1/2/x/y1/y2/z", true});
		cases.add(new Object[]{"x/**/z/**", "x/y1/y2/z", true});
		cases.add(new Object[]{"x/**/z/**", "x/y1/y2/z/1", true});
		cases.add(new Object[]{"x/**/z/**", "x/y1/y2/z/1/2", true});
		cases.add(new Object[]{"x/**/$/**", "x/y1/y2/$/1/2", true});
		cases.add(new Object[]{"x/**/$$/**", "x/y1/y2/$$/1/2", true});
		return cases;
	}
	
	String glob;
	String line;
	boolean match;
	
	public GlobHelperTest(String glob, String line, boolean match) {
		super();
		this.glob = glob;
		this.line = line;
		this.match = match;
	}

	@Test
	public void verify_match() {
		Pattern pattern = GlobHelper.translate(glob, "\\/");
		boolean matches = pattern.matcher(line).matches();
		Assert.assertTrue(glob + " match " + line + " -> " + match, matches == match);
	}
	
}
