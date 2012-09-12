package org.gridkit.vicluster.isolate;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import org.gridkit.util.concurrent.FutureEx;
import org.gridkit.vicluster.isolate.Isolate.FutureProxy;
import org.gridkit.zerormi.DuplexBlobPipe;
import org.gridkit.zerormi.RmiFactory;
import org.gridkit.zerormi.RmiGateway;
import org.gridkit.zerormi.SimpleClassProvider;
import org.gridkit.zerormi.SmartRmiMarshaler;

/**
 * This class bridges {@link RmiGateway} from inside of isolate,
 * providing cross isolate interface.
 * 
 * This way isolate could function, through ZeroRMI library is isolated.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class IsolateRmiFacility implements Isolate.RmiFacility {

	RmiGateway gateway;
	
	@Override
	public void startRmi(String name, ClassLoader cl, Isolate.BlobDuplex bd, Executor exec) {		
		DuplexBlobPipe duplex = new IsolateBlobPipeWrapper(bd);
		gateway = RmiFactory.createEndPoint(name, new SimpleClassProvider(cl), duplex, exec, new SmartRmiMarshaler());
	}

	@Override
	public FutureProxy<Void> submit(Runnable task) {
		return new IsolateFutures.FutureEnproxy<Void>(gateway.asExecutor().submit(task));
	}

	@Override
	public <V> FutureProxy<V> submit(Callable<V> task) {
		return new IsolateFutures.FutureEnproxy<V>(gateway.asExecutor().submit(task));
	}

	@Override
	public void stop() {
		gateway.shutdown();		
	}
}
