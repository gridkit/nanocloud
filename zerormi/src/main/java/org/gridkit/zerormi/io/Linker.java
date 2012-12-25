package org.gridkit.zerormi.io;

import org.gridkit.zerormi.io.ByteStreamInputPin;
import org.gridkit.zerormi.io.ByteStreamOutputPin;
import org.gridkit.zerormi.io.ByteStreamSink;
import org.gridkit.zerormi.io.ByteStreamSource;

public class Linker {

	public boolean isLinkable(ByteStreamInputPin input, ByteStreamOutputPin output) {
		
	}
	
	public void link(ByteStreamInputPin input, ByteStreamOutputPin output) {
		if (output.isPassiveModeSupported() && input.isActiveModeSupported()) {
			input.linkToSink(output.linkAsSink());
		}
		else if (input.isPassiveModeSupported() && output.isActiveModeSupported()) {
			input.linkToSink(output.linkAsSink());
		}
		else {
			throw new IllegalAccessException();
		}
	}
	
	public void linkToSink(ByteStreamInputPin input, ByteStreamSink sink) {
		
	}

	public ByteStreamSource linkAsSource(ByteStreamInputPin input) {
		
	}

	public void linkToSource(ByteStreamOutputPin output, ByteStreamSource sink) {
		
	}

	public ByteStreamSink linkAsSink(ByteStreamOutputPin output) {
		
	}

}
