package org.gridkit.zerormi;

import java.util.concurrent.TimeUnit;

public interface CoWorker {

	public void addListener(CoWorkerListener listener);

	public void removeListener(CoWorkerListener listener);
	
	public boolean isReady();
	
	public void process(long time, TimeUnit tu) throws InterruptedException;

	public interface CoWorkerListener {
		
		public void notify(CoWorker worker);

		public void notify(CoWorker worker, long delay, TimeUnit tu);
		
	}
}
