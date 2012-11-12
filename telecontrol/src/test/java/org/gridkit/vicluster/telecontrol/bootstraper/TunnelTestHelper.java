package org.gridkit.vicluster.telecontrol.bootstraper;

import org.gridkit.vicluster.telecontrol.bootstraper.Tunneller;

public class TunnelTestHelper {

	public static void enableChannelTrace(Tunneller t) {
		t.traceChannelOpen = true;
		t.traceChannelData = true;
		t.traceChannelClose = true;
	}

	public static void enableChannelTrace(TunnellerConnection t) {
		t.traceChannelOpen = true;
		t.traceChannelData = true;
		t.traceChannelClose = true;
	}
	
}
