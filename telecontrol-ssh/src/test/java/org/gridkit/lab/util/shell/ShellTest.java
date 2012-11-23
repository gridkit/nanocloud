package org.gridkit.lab.util.shell;

import java.io.IOException;

import org.junit.Test;

public class ShellTest {

	@Test
	public void test() throws IOException, InterruptedException {
		Prompt p = Shell.prompt();
		p.cd("{temp}");
		for(String s: p.find("**/*.exe")) {
			System.out.println(s);
		}
	}

}
