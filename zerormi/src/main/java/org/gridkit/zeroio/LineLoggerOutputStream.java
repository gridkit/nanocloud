package org.gridkit.zeroio;

import java.io.IOException;

import org.gridkit.zerormi.zlog.LogStream;

public class LineLoggerOutputStream extends AbstractLineProcessingOutputStream {

	private String prefix;
	private LogStream stream;

	public LineLoggerOutputStream(LogStream stream) {
		this("", stream);
	}

	public LineLoggerOutputStream(String prefix, LogStream stream) {
		this.prefix = prefix;
		this.stream = stream;
	}

	@Override
	protected void processLine(byte[] data) throws IOException {
		String line = new String(data);
		while(line.endsWith("\n") || line.endsWith("\r")) {
			line = line.substring(0, line.length() - 1);
		}
		stream.log(prefix + line);
	}
}
