package org.gridkit.nanocloud.telecontrol;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gridkit.vicluster.telecontrol.StreamPipe;
import org.gridkit.vicluster.telecontrol.bootstraper.Tunneller;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection;
import org.junit.After;
import org.junit.Before;

public class TunnelerControlConsoleTest extends LocalControlConsoleTest {

	private Thread tunnellerThread;
	
	
	@Before
	public void initConsole() throws IOException, InterruptedException, TimeoutException {
		final StreamPipe ib = new StreamPipe(4 << 1000);
		final StreamPipe ob = new StreamPipe(4 << 1000);
		final Tunneller tunneller = new Tunneller();
		tunnellerThread = new Thread("TUNNELLER") {
			@Override
			public void run() {
				tunneller.process(ib.getInputStream(), ob.getOutputStream());
			}
		};
		tunnellerThread.start();
		TunnellerConnection conn = new TunnellerConnection("Test", ob.getInputStream(), ib.getOutputStream(), System.out, 10, TimeUnit.SECONDS);
		console = new TunnellerControlConsole(conn, "target/.tunneler");
	}

	@After
	public void destroyConsole() {
		tunnellerThread.interrupt();
		console.terminate();
	}
}
