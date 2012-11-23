package org.gridkit.lab.util.shell;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class TailerTest {

	@Test
	public void test() throws IOException, InterruptedException {
		Tailer tailer = new Tailer(new File("c:\\\\log.txt"));
		while(true) {
			String line = tailer.nextLine();
			if (line == null) {
				Thread.sleep(1000);
			}
			else {
				System.out.println(line);
			}
		}
	}

}
