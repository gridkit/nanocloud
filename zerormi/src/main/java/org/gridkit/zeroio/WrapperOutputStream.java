package org.gridkit.zeroio;

import java.io.IOException;
import java.io.PrintStream;

/**
 * @author Alexey Ragozin
 */
public class WrapperOutputStream extends AbstractLineProcessingOutputStream {

	private String prefix;
	private PrintStream printStream;
	
	public WrapperOutputStream(String prefix, PrintStream printStream) {
		this.prefix = prefix;
		this.printStream = printStream;
	}

	@Override
	protected void processLine(byte[] data) throws IOException {
		printStream.print(prefix + new String(data));		
	}
}