package org.gridkit.util.concurrent.zerormi;

import java.rmi.Remote;

import org.gridkit.util.concurrent.BlockingBarrier;
import org.gridkit.util.concurrent.Latch;
import org.gridkit.util.concurrent.LatchBarrier;

public class DistributedBarriers {

	public RemoteLatchBarrier export(LatchBarrier barrier) {
		
	}
	
	public static interface RemoteLatchBarrier extends BlockingBarrier, Latch, Remote {		
	}
}
