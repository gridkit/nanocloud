package org.gridkit.vicluster.telecontrol.jvm;

import java.io.IOException;
import java.io.OutputStream;

class SplittingOutputStream extends OutputStream {

	private boolean silence = false; 
	private OutputStream[] outs;
	
	public SplittingOutputStream(OutputStream... outs) {
		this.outs = outs;
	}

	public synchronized void silence() {
		try {
			flush();
			close();
		}
		catch(IOException e) {
			// ignore
		}
	}
	
	@Override
	public synchronized void write(int b) throws IOException {
		if (!silence) {
			for(OutputStream a: outs) {
				a.write(b);
			}
		}
	}

	@Override
	public synchronized void write(byte[] b) throws IOException {
		if (!silence) {
			for(OutputStream a: outs) {
				a.write(b);
			}
		}
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		if (!silence) {
			for(OutputStream a: outs) {
				a.write(b, off, len);
			}
		}
	}

	@Override
	public synchronized void flush() throws IOException {
		if (!silence) {
			for(OutputStream a: outs) {
				a.flush();
			}
		}
	}

	@Override
	public synchronized void close() throws IOException {
		if (!silence) {
			silence = true;
			for(OutputStream a: outs) {
				a.close();
			}
		}		
	}
}
