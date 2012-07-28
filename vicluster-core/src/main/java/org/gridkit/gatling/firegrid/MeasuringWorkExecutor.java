package org.gridkit.gatling.firegrid;

import java.io.Serializable;

import org.gridkit.gatling.stats.PerfSample;
import org.gridkit.gatling.stats.PerfSampleBlock;

@SuppressWarnings("serial")
public class MeasuringWorkExecutor implements WorkExecutor, Serializable {

	private WorkExecutor functionalExecutor;
	private WorkStatisticsReceiver statisticsCallback;
	
	private PerfSampleBlock statBlock;
	
	public MeasuringWorkExecutor(WorkExecutor functionalExecutor, WorkStatisticsReceiver receiver) {
		this.functionalExecutor = functionalExecutor;
		this.statisticsCallback = receiver;
	}
	
	@Override
	public void initialize() {
		functionalExecutor.initialize();		
	}
	
	@Override
	public void initWorkPacket(WorkPacket packet) {
		functionalExecutor.initWorkPacket(packet);
		
		this.statBlock = new PerfSampleBlock(packet.getPacketSize());		
	}
	
	@Override
	public int execute() {
		long timestamp = System.currentTimeMillis();
		long start = System.nanoTime();
		int result = functionalExecutor.execute();
		long time = System.nanoTime() - start;
		PerfSample sample = new PerfSample();
		sample.timestamp = timestamp;
		sample.duration = time;
		sample.result = result;		
		statBlock.addSample(sample);
		return result;
	}
	
	@Override
	public void finishWorkPacket(WorkPacket packet) {
		functionalExecutor.finishWorkPacket(packet);
		final PerfSampleBlock completedBlock = statBlock;
		packet = null;
		statBlock = null;
		
		Runnable task = new Runnable() {			
			@Override
			public void run() {
				try {
					completedBlock.host = DaemonIdentity.LOCAL;
					statisticsCallback.sendStatistics(completedBlock);
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
		};		
		new Thread(task).run();
	}	
	
	@Override
	public void shutdown() {
		functionalExecutor.shutdown();
	}
}
