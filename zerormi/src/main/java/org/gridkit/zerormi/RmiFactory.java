package org.gridkit.zerormi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import org.gridkit.zerormi.ByteStream.Duplex;
import org.slf4j.LoggerFactory;


public class RmiFactory {

	public static RmiGateway createEndPoint(String name, ClassProvider cp, DuplexBlobPipe blobPipe, Executor executor, RmiMarshaler... extraMarshaler) {
		
		List<RmiMarshaler> mlist = new ArrayList<RmiMarshaler>();
		
		StopAllSuperviser superviser = new StopAllSuperviser();
		
		RmiChannel2 channel = new RmiChannel2(name, superviser, cp, executor);
		
		mlist.addAll(Arrays.asList(extraMarshaler));
		// channel should be last
		mlist.add(channel);
		
		RmiMarshalStack rm = new RmiMarshalStack(mlist.toArray(new RmiMarshaler[mlist.size()]));
		
		SmartObjectSerializer objectPipe = new SmartObjectSerializer(blobPipe, rm, cp);
		
		superviser.addComponent(channel);
		superviser.addComponent(blobPipe);
		superviser.addComponent(objectPipe);
		
		channel.setPipe(objectPipe);
		
		return new RmiEndPointAdapter(channel);
	}
	
	private static class StopAllSuperviser implements ReliableBlobPipe.PipeSuperviser {

		private List<Object> components = new ArrayList<Object>();
		private boolean terminated;
		
		public synchronized void addComponent(Object component) {
			components.add(component);
		}
		
		@Override
		public void onWarning(SuperviserEvent event) {
			logWarn(event);
		}

		@Override
		public void onTermination(SuperviserEvent event) {
			synchronized(this) {
				logInfo(event);
				if (terminated) {
					return;
				}
				else {
					terminated = true;
				}
			}			
		}

		@Override
		public void onFatalError(SuperviserEvent event) {
			synchronized(this) {
				logError(event);
				if (terminated) {
					return;
				}
				else {
					terminated = true;
				}
			}
			stopAll();
		}

		private void logInfo(SuperviserEvent event) {
			LoggerFactory.getLogger(event.getComponent().getClass()).info(event.toString());
		}
		
		private void logWarn(SuperviserEvent event) {
			LoggerFactory.getLogger(event.getComponent().getClass()).warn(event.toString());
		}

		private void logError(SuperviserEvent event) {
			LoggerFactory.getLogger(event.getComponent().getClass()).error(event.toString());
		}

		private void stopAll() {
			for(Object obj: components) {
				stop(obj);
			}			
		}

		@Override		
		public void onStreamRejected(ReliableBlobPipe pipe, Duplex stream,	Exception e) {
			onFatalError(SuperviserEvent.newUnexpectedError(pipe, e));
		}
	}
	
	private static void stop(Object obj) {
		if (obj instanceof RmiChannel2) {
			((RmiChannel2)obj).destroy();
		}
		else if (obj instanceof DuplexObjectPipe) {
			((DuplexObjectPipe)obj).close();
		}
		else if (obj instanceof DuplexBlobPipe) {
			((DuplexBlobPipe)obj).close();
		}
		else {
			throw new RuntimeException("Cannot stop " + obj);
		}
	}
}
