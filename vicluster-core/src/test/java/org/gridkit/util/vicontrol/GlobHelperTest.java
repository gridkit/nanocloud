package org.gridkit.util.vicontrol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;

import org.gridkit.bjtest.BetterParameterized;
import org.gridkit.bjtest.BetterParameterized.Parameters;
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
		cases.add(new Object[]{"x/**/z", "x/y/z", true});
		cases.add(new Object[]{"x/*/z", "x/y1/y2/z", false});
		cases.add(new Object[]{"x/**/z", "x/y1/y2/z", true});
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
		Assert.assertTrue(glob + " match " + line + " -> " + match, GlobHelper.translate(glob, "\\/").matcher(line).matches() == match);
	}
	
}
