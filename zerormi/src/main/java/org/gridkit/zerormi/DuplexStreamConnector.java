package org.gridkit.zerormi;

import java.io.IOException;

public interface DuplexStreamConnector {

	public DuplexStream connect() throws IOException;
	
}
